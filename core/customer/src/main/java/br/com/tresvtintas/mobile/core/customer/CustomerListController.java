package br.com.tresvtintas.mobile.core.customer;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class CustomerListController {
    private final CustomerRepository repository;
    private final Executor workerExecutor;
    private final Executor mainExecutor;
    private final Set<CustomerListStateListener> listeners =
            new CopyOnWriteArraySet<>();
    private final AtomicBoolean operationInProgress = new AtomicBoolean();
    private final AtomicLong generation = new AtomicLong();
    private volatile CustomerListState current = CustomerListState.empty();
    private volatile CustomerQuery query = CustomerQuery.initial();

    public CustomerListController(
            CustomerRepository repository,
            Executor workerExecutor,
            Executor mainExecutor) {
        this.repository = Objects.requireNonNull(
                repository,
                "Customer repository is required.");
        this.workerExecutor = Objects.requireNonNull(
                workerExecutor,
                "Worker executor is required.");
        this.mainExecutor = Objects.requireNonNull(
                mainExecutor,
                "Main executor is required.");
    }

    public void subscribe(CustomerListStateListener listener) {
        CustomerListStateListener required = Objects.requireNonNull(
                listener,
                "Customer listener is required.");
        listeners.add(required);
        CustomerListState snapshot = current;
        mainExecutor.execute(() -> required.onCustomerListStateChanged(snapshot));
    }

    public void unsubscribe(CustomerListStateListener listener) {
        listeners.remove(listener);
    }

    public CustomerListState currentState() {
        return current;
    }

    public CustomerQuery currentQuery() {
        return query;
    }

    public void open(CustomerQuery requestedQuery) {
        query = Objects.requireNonNull(
                requestedQuery,
                "Customer query is required.");
        long operationGeneration = generation.incrementAndGet();
        operationInProgress.set(true);
        publish(CustomerListState.loading());
        workerExecutor.execute(() -> executeFirstPage(
                requestedQuery,
                operationGeneration,
                Optional.empty()));
    }

    public void refresh() {
        CustomerListState state = current;
        if (state.phase() == CustomerListState.Phase.CLOSED
                || !operationInProgress.compareAndSet(false, true)) {
            return;
        }
        long operationGeneration = generation.incrementAndGet();
        Optional<CustomerSnapshot> fallback = state.snapshot();
        fallback.ifPresentOrElse(
                value -> publish(CustomerListState.refreshing(value)),
                () -> publish(CustomerListState.loading()));
        workerExecutor.execute(() ->
                executeFirstPage(query, operationGeneration, fallback));
    }

    public void loadMore() {
        CustomerListState state = current;
        Optional<CustomerSnapshot> snapshot = state.snapshot();
        if (snapshot.isEmpty()
                || !snapshot.orElseThrow().hasMore()
                || !operationInProgress.compareAndSet(false, true)) {
            return;
        }
        long operationGeneration = generation.incrementAndGet();
        CustomerSnapshot currentSnapshot = snapshot.orElseThrow();
        publish(CustomerListState.loadingMore(currentSnapshot));
        workerExecutor.execute(() ->
                executeNextPage(query, operationGeneration, currentSnapshot));
    }

    public void close() {
        generation.incrementAndGet();
        operationInProgress.set(false);
        publish(CustomerListState.closed());
        listeners.clear();
    }

    private void executeFirstPage(
            CustomerQuery requestedQuery,
            long operationGeneration,
            Optional<CustomerSnapshot> fallback) {
        try {
            CustomerPage page = repository.page(
                    requestedQuery,
                    Optional.empty());
            completeReady(
                    operationGeneration,
                    CustomerSnapshot.from(page));
        } catch (CustomerException exception) {
            completeFailure(operationGeneration, fallback, exception);
        }
    }

    private void executeNextPage(
            CustomerQuery requestedQuery,
            long operationGeneration,
            CustomerSnapshot currentSnapshot) {
        try {
            CustomerPage page = repository.page(
                    requestedQuery,
                    currentSnapshot.nextCursor());
            completeReady(
                    operationGeneration,
                    currentSnapshot.append(page));
        } catch (CustomerException exception) {
            completeFailure(
                    operationGeneration,
                    Optional.of(currentSnapshot),
                    exception);
        }
    }

    private void completeReady(
            long operationGeneration,
            CustomerSnapshot snapshot) {
        if (!isCurrent(operationGeneration)) {
            return;
        }
        operationInProgress.set(false);
        publish(CustomerListState.ready(
                snapshot,
                Optional.empty(),
                Optional.empty()));
    }

    private void completeFailure(
            long operationGeneration,
            Optional<CustomerSnapshot> fallback,
            CustomerException exception) {
        if (!isCurrent(operationGeneration)) {
            return;
        }
        operationInProgress.set(false);
        if (fallback.isPresent() && mayExposeFallback(exception.kind())) {
            publish(CustomerListState.ready(
                    fallback.orElseThrow(),
                    Optional.of(exception.kind()),
                    exception.requestId()));
            return;
        }
        publish(CustomerListState.error(exception));
    }

    private boolean isCurrent(long operationGeneration) {
        return generation.get() == operationGeneration
                && current.phase() != CustomerListState.Phase.CLOSED;
    }

    private void publish(CustomerListState next) {
        current = next;
        mainExecutor.execute(() -> {
            for (CustomerListStateListener listener : listeners) {
                listener.onCustomerListStateChanged(next);
            }
        });
    }

    private static boolean mayExposeFallback(CustomerFailureKind kind) {
        return switch (kind) {
            case NETWORK, RATE_LIMITED, SERVICE_UNAVAILABLE -> true;
            default -> false;
        };
    }
}
