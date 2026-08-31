package br.com.tresvtintas.mobile.core.delivery;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class DeliveryListController {
    @FunctionalInterface
    public interface Listener {
        void onDeliveryListStateChanged(DeliveryListState state);
    }

    private final DeliveryRepository repository;
    private final Executor worker;
    private final Executor main;
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final AtomicLong generation = new AtomicLong();
    private volatile DeliveryListState current = DeliveryListState.empty();
    private volatile DeliveryQuery query = DeliveryQuery.initial();

    public DeliveryListController(
            DeliveryRepository repository,
            Executor worker,
            Executor main) {
        this.repository = Objects.requireNonNull(repository, "Delivery repository is required.");
        this.worker = Objects.requireNonNull(worker, "Delivery worker is required.");
        this.main = Objects.requireNonNull(main, "Main executor is required.");
    }

    public void subscribe(Listener listener) {
        Listener required = Objects.requireNonNull(listener, "Delivery listener is required.");
        listeners.add(required);
        DeliveryListState snapshot = current;
        main.execute(() -> required.onDeliveryListStateChanged(snapshot));
    }

    public void unsubscribe(Listener listener) {
        listeners.remove(listener);
    }

    public DeliveryQuery currentQuery() {
        return query;
    }

    public void open(DeliveryQuery requested) {
        query = Objects.requireNonNull(requested, "Delivery query is required.");
        long operation = generation.incrementAndGet();
        busy.set(true);
        publish(DeliveryListState.loading());
        worker.execute(() -> first(requested, operation, Optional.empty()));
    }

    public void refresh() {
        if (current.phase() == DeliveryListState.Phase.CLOSED
                || !busy.compareAndSet(false, true)) {
            return;
        }
        long operation = generation.incrementAndGet();
        Optional<DeliveryListState.Snapshot> fallback = current.snapshot();
        fallback.ifPresentOrElse(
                value -> publish(DeliveryListState.refreshing(value)),
                () -> publish(DeliveryListState.loading()));
        worker.execute(() -> first(query, operation, fallback));
    }

    public void loadMore() {
        Optional<DeliveryListState.Snapshot> snapshot = current.snapshot();
        if (snapshot.isEmpty()
                || !snapshot.orElseThrow().hasMore()
                || !busy.compareAndSet(false, true)) {
            return;
        }
        long operation = generation.incrementAndGet();
        DeliveryListState.Snapshot value = snapshot.orElseThrow();
        publish(DeliveryListState.loadingMore(value));
        worker.execute(() -> next(query, operation, value));
    }

    public void close() {
        generation.incrementAndGet();
        busy.set(false);
        publish(DeliveryListState.closed());
        listeners.clear();
    }

    private void first(
            DeliveryQuery requested,
            long operation,
            Optional<DeliveryListState.Snapshot> fallback) {
        try {
            ready(operation, DeliveryListState.Snapshot.from(
                    repository.page(requested, Optional.empty())));
        } catch (DeliveryException failure) {
            failed(operation, fallback, failure);
        }
    }

    private void next(
            DeliveryQuery requested,
            long operation,
            DeliveryListState.Snapshot snapshot) {
        try {
            ready(operation, snapshot.append(
                    repository.page(requested, snapshot.nextCursor())));
        } catch (DeliveryException failure) {
            failed(operation, Optional.of(snapshot), failure);
        }
    }

    private void ready(long operation, DeliveryListState.Snapshot snapshot) {
        if (!isCurrent(operation)) {
            return;
        }
        busy.set(false);
        publish(DeliveryListState.ready(
                snapshot,
                Optional.empty(),
                Optional.empty()));
    }

    private void failed(
            long operation,
            Optional<DeliveryListState.Snapshot> fallback,
            DeliveryException failure) {
        if (!isCurrent(operation)) {
            return;
        }
        busy.set(false);
        if (fallback.isPresent() && isTransient(failure.kind())) {
            publish(DeliveryListState.ready(
                    fallback.orElseThrow(),
                    Optional.of(failure.kind()),
                    failure.requestId()));
        } else {
            publish(DeliveryListState.error(failure));
        }
    }

    private boolean isCurrent(long operation) {
        return generation.get() == operation
                && current.phase() != DeliveryListState.Phase.CLOSED;
    }

    private static boolean isTransient(DeliveryFailureKind failure) {
        return failure == DeliveryFailureKind.NETWORK
                || failure == DeliveryFailureKind.RATE_LIMITED
                || failure == DeliveryFailureKind.SERVICE_UNAVAILABLE;
    }

    private void publish(DeliveryListState state) {
        current = state;
        main.execute(() -> listeners.forEach(
                listener -> listener.onDeliveryListStateChanged(state)));
    }
}
