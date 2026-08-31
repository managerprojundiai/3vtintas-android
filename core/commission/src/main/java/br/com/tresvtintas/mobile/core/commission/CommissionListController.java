package br.com.tresvtintas.mobile.core.commission;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class CommissionListController {
    private final CommissionRepository repository;
    private final Executor worker;
    private final Executor main;
    private final Set<CommissionListStateListener> listeners =
            new CopyOnWriteArraySet<>();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final AtomicLong generation = new AtomicLong();
    private volatile CommissionListState current = CommissionListState.empty();
    private volatile CommissionQuery query;

    public CommissionListController(
            CommissionRepository repository,
            CommissionQuery initialQuery,
            Executor worker,
            Executor main) {
        this.repository = Objects.requireNonNull(
                repository,
                "Commission repository is required.");
        this.query = Objects.requireNonNull(
                initialQuery,
                "Commission query is required.");
        this.worker = Objects.requireNonNull(worker, "Commission worker is required.");
        this.main = Objects.requireNonNull(main, "Main executor is required.");
    }

    public void subscribe(CommissionListStateListener listener) {
        CommissionListStateListener required = Objects.requireNonNull(
                listener,
                "Commission listener is required.");
        listeners.add(required);
        CommissionListState snapshot = current;
        main.execute(() -> required.onCommissionListStateChanged(snapshot));
    }

    public void unsubscribe(CommissionListStateListener listener) {
        listeners.remove(listener);
    }

    public CommissionQuery currentQuery() {
        return query;
    }

    public void open(CommissionQuery requested) {
        query = Objects.requireNonNull(requested, "Commission query is required.");
        long operation = generation.incrementAndGet();
        busy.set(true);
        publish(CommissionListState.loading());
        worker.execute(() -> first(requested, operation, Optional.empty()));
    }

    public void refresh() {
        if (current.phase() == CommissionListState.Phase.CLOSED
                || !busy.compareAndSet(false, true)) {
            return;
        }
        long operation = generation.incrementAndGet();
        Optional<CommissionSnapshot> fallback = current.snapshot();
        fallback.ifPresentOrElse(
                value -> publish(CommissionListState.refreshing(value)),
                () -> publish(CommissionListState.loading()));
        worker.execute(() -> first(query, operation, fallback));
    }

    public void loadMore() {
        Optional<CommissionSnapshot> snapshot = current.snapshot();
        if (snapshot.isEmpty()
                || !snapshot.orElseThrow().hasMore()
                || !busy.compareAndSet(false, true)) {
            return;
        }
        long operation = generation.incrementAndGet();
        CommissionSnapshot value = snapshot.orElseThrow();
        publish(CommissionListState.loadingMore(value));
        worker.execute(() -> next(query, operation, value));
    }

    public void close() {
        generation.incrementAndGet();
        busy.set(false);
        publish(CommissionListState.closed());
        listeners.clear();
    }

    private void first(
            CommissionQuery requested,
            long operation,
            Optional<CommissionSnapshot> fallback) {
        try {
            ready(
                    operation,
                    CommissionSnapshot.from(
                            repository.page(requested, Optional.empty())));
        } catch (CommissionException failure) {
            failed(operation, fallback, failure);
        }
    }

    private void next(
            CommissionQuery requested,
            long operation,
            CommissionSnapshot snapshot) {
        try {
            ready(
                    operation,
                    snapshot.append(repository.page(
                            requested,
                            snapshot.nextCursor())));
        } catch (CommissionException failure) {
            failed(operation, Optional.of(snapshot), failure);
        }
    }

    private void ready(long operation, CommissionSnapshot snapshot) {
        if (!current(operation)) {
            return;
        }
        busy.set(false);
        publish(CommissionListState.ready(
                snapshot,
                Optional.empty(),
                Optional.empty()));
    }

    private void failed(
            long operation,
            Optional<CommissionSnapshot> fallback,
            CommissionException failure) {
        if (!current(operation)) {
            return;
        }
        busy.set(false);
        if (fallback.isPresent()
                && switch (failure.kind()) {
                    case NETWORK, RATE_LIMITED, SERVICE_UNAVAILABLE -> true;
                    default -> false;
                }) {
            publish(CommissionListState.ready(
                    fallback.orElseThrow(),
                    Optional.of(failure.kind()),
                    failure.requestId()));
        } else {
            publish(CommissionListState.error(failure));
        }
    }

    private boolean current(long operation) {
        return generation.get() == operation
                && current.phase() != CommissionListState.Phase.CLOSED;
    }

    private void publish(CommissionListState state) {
        current = state;
        main.execute(() -> listeners.forEach(
                listener -> listener.onCommissionListStateChanged(state)));
    }
}
