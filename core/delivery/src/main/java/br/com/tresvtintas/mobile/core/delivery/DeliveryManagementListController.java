package br.com.tresvtintas.mobile.core.delivery;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class DeliveryManagementListController {
    @FunctionalInterface
    public interface Listener {
        void onDeliveryManagementListStateChanged(
                DeliveryManagementListState state);
    }

    private final DeliveryManagementRepository repository;
    private final Executor worker;
    private final Executor main;
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final AtomicLong generation = new AtomicLong();
    private volatile DeliveryManagementListState current =
            DeliveryManagementListState.empty();

    public DeliveryManagementListController(
            DeliveryManagementRepository repository,
            Executor worker,
            Executor main) {
        this.repository = Objects.requireNonNull(
                repository,
                "Management repository is required.");
        this.worker = Objects.requireNonNull(worker, "Worker is required.");
        this.main = Objects.requireNonNull(main, "Main executor is required.");
    }

    public void subscribe(Listener listener) {
        Listener required = Objects.requireNonNull(
                listener,
                "Management listener is required.");
        listeners.add(required);
        DeliveryManagementListState snapshot = current;
        main.execute(() ->
                required.onDeliveryManagementListStateChanged(snapshot));
    }

    public void unsubscribe(Listener listener) {
        listeners.remove(listener);
    }

    public Optional<DeliveryManagementQuery> currentQuery() {
        return current.snapshot().map(
                DeliveryManagementListState.Snapshot::query);
    }

    public void open() {
        long operation = generation.incrementAndGet();
        busy.set(true);
        publish(DeliveryManagementListState.loading());
        worker.execute(() -> loadDirectory(operation));
    }

    public void selectOrganization(long organizationId) {
        Optional<DeliveryManagementListState.Snapshot> snapshot =
                current.snapshot();
        if (snapshot.isEmpty()) {
            return;
        }
        DeliveryManagementOrganization selected = snapshot.orElseThrow()
                .organizations()
                .stream()
                .filter(value -> value.id() == organizationId)
                .findFirst()
                .orElse(null);
        if (selected == null) {
            return;
        }
        openQuery(
                DeliveryManagementQuery.initial(selected.id()),
                snapshot.orElseThrow().organizations());
    }

    public void openQuery(DeliveryManagementQuery query) {
        current.snapshot().ifPresent(snapshot ->
                openQuery(query, snapshot.organizations()));
    }

    public void refresh() {
        Optional<DeliveryManagementListState.Snapshot> snapshot =
                current.snapshot();
        if (snapshot.isEmpty()
                || !busy.compareAndSet(false, true)) {
            return;
        }
        long operation = generation.incrementAndGet();
        DeliveryManagementListState.Snapshot fallback =
                snapshot.orElseThrow();
        publish(DeliveryManagementListState.refreshing(fallback));
        worker.execute(() -> first(
                fallback.query(),
                fallback.organizations(),
                operation,
                Optional.of(fallback)));
    }

    public void loadMore() {
        Optional<DeliveryManagementListState.Snapshot> snapshot =
                current.snapshot();
        if (snapshot.isEmpty()
                || !snapshot.orElseThrow().hasMore()
                || !busy.compareAndSet(false, true)) {
            return;
        }
        long operation = generation.incrementAndGet();
        DeliveryManagementListState.Snapshot value =
                snapshot.orElseThrow();
        publish(DeliveryManagementListState.loadingMore(value));
        worker.execute(() -> next(operation, value));
    }

    public void close() {
        generation.incrementAndGet();
        busy.set(false);
        publish(DeliveryManagementListState.closed());
        listeners.clear();
    }

    private void loadDirectory(long operation) {
        try {
            List<DeliveryManagementOrganization> organizations =
                    repository.organizations();
            if (organizations.isEmpty()) {
                throw new DeliveryException(
                        DeliveryFailureKind.FORBIDDEN,
                        "No delivery management organization is available.");
            }
            DeliveryManagementQuery query =
                    DeliveryManagementQuery.initial(
                            organizations.get(0).id());
            first(query, organizations, operation, Optional.empty());
        } catch (DeliveryException failure) {
            failed(operation, Optional.empty(), failure);
        }
    }

    private void openQuery(
            DeliveryManagementQuery query,
            List<DeliveryManagementOrganization> organizations) {
        if (!busy.compareAndSet(false, true)) {
            return;
        }
        long operation = generation.incrementAndGet();
        publish(DeliveryManagementListState.loading());
        worker.execute(() -> first(
                query,
                organizations,
                operation,
                Optional.empty()));
    }

    private void first(
            DeliveryManagementQuery query,
            List<DeliveryManagementOrganization> organizations,
            long operation,
            Optional<DeliveryManagementListState.Snapshot> fallback) {
        try {
            DeliveryManagementOrganization selected = organizations.stream()
                    .filter(value -> value.id() == query.organizationId())
                    .findFirst()
                    .orElseThrow();
            DeliveryManagementPage page =
                    repository.page(query, Optional.empty());
            ready(operation, new DeliveryManagementListState.Snapshot(
                    organizations,
                    selected,
                    query,
                    page.items(),
                    page.nextCursor()));
        } catch (DeliveryException failure) {
            failed(operation, fallback, failure);
        }
    }

    private void next(
            long operation,
            DeliveryManagementListState.Snapshot snapshot) {
        try {
            ready(operation, snapshot.append(repository.page(
                    snapshot.query(),
                    snapshot.nextCursor())));
        } catch (DeliveryException failure) {
            failed(operation, Optional.of(snapshot), failure);
        }
    }

    private void ready(
            long operation,
            DeliveryManagementListState.Snapshot snapshot) {
        if (!isCurrent(operation)) {
            return;
        }
        busy.set(false);
        publish(DeliveryManagementListState.ready(snapshot));
    }

    private void failed(
            long operation,
            Optional<DeliveryManagementListState.Snapshot> fallback,
            DeliveryException failure) {
        if (!isCurrent(operation)) {
            return;
        }
        busy.set(false);
        if (fallback.isPresent() && isTransient(failure.kind())) {
            publish(DeliveryManagementListState.warning(
                    fallback.orElseThrow(),
                    failure));
        } else {
            publish(DeliveryManagementListState.error(failure));
        }
    }

    private boolean isCurrent(long operation) {
        return generation.get() == operation
                && current.phase()
                        != DeliveryManagementListState.Phase.CLOSED;
    }

    private static boolean isTransient(DeliveryFailureKind failure) {
        return failure == DeliveryFailureKind.NETWORK
                || failure == DeliveryFailureKind.RATE_LIMITED
                || failure == DeliveryFailureKind.SERVICE_UNAVAILABLE;
    }

    private void publish(DeliveryManagementListState state) {
        current = state;
        main.execute(() -> listeners.forEach(listener ->
                listener.onDeliveryManagementListStateChanged(state)));
    }
}
