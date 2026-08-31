package br.com.tresvtintas.mobile.core.finance;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class FinanceListController {
    private final FinanceRepository repository;
    private final Executor worker;
    private final Executor main;
    private final Set<FinanceListStateListener> listeners =
            new CopyOnWriteArraySet<>();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final AtomicLong generation = new AtomicLong();
    private volatile FinanceListState current = FinanceListState.empty();
    private volatile FinanceQuery query;

    public FinanceListController(
            FinanceRepository repository,
            FinanceQuery initialQuery,
            Executor worker,
            Executor main) {
        this.repository = Objects.requireNonNull(
                repository,
                "Finance repository is required.");
        this.query = Objects.requireNonNull(
                initialQuery,
                "Finance query is required.");
        this.worker = Objects.requireNonNull(worker, "Finance worker is required.");
        this.main = Objects.requireNonNull(main, "Main executor is required.");
    }

    public void subscribe(FinanceListStateListener listener) {
        FinanceListStateListener required = Objects.requireNonNull(
                listener,
                "Finance listener is required.");
        listeners.add(required);
        FinanceListState snapshot = current;
        main.execute(() -> required.onFinanceListStateChanged(snapshot));
    }

    public void unsubscribe(FinanceListStateListener listener) {
        listeners.remove(listener);
    }

    public FinanceQuery currentQuery() {
        return query;
    }

    public void open(FinanceQuery requested) {
        query = Objects.requireNonNull(requested, "Finance query is required.");
        long operation = generation.incrementAndGet();
        busy.set(true);
        publish(FinanceListState.loading());
        worker.execute(() -> first(requested, operation, Optional.empty()));
    }

    public void refresh() {
        if (current.phase() == FinanceListState.Phase.CLOSED
                || !busy.compareAndSet(false, true)) {
            return;
        }
        long operation = generation.incrementAndGet();
        Optional<FinanceSnapshot> fallback = current.snapshot();
        fallback.ifPresentOrElse(
                value -> publish(FinanceListState.refreshing(value)),
                () -> publish(FinanceListState.loading()));
        worker.execute(() -> first(query, operation, fallback));
    }

    public void loadMore() {
        Optional<FinanceSnapshot> snapshot = current.snapshot();
        if (snapshot.isEmpty()
                || !snapshot.orElseThrow().hasMore()
                || !busy.compareAndSet(false, true)) {
            return;
        }
        long operation = generation.incrementAndGet();
        FinanceSnapshot value = snapshot.orElseThrow();
        publish(FinanceListState.loadingMore(value));
        worker.execute(() -> next(query, operation, value));
    }

    public void close() {
        generation.incrementAndGet();
        busy.set(false);
        publish(FinanceListState.closed());
        listeners.clear();
    }

    private void first(
            FinanceQuery requested,
            long operation,
            Optional<FinanceSnapshot> fallback) {
        try {
            ready(
                    operation,
                    FinanceSnapshot.from(repository.page(
                            requested,
                            Optional.empty())));
        } catch (FinanceException failure) {
            failed(operation, fallback, failure);
        }
    }

    private void next(
            FinanceQuery requested,
            long operation,
            FinanceSnapshot snapshot) {
        try {
            ready(
                    operation,
                    snapshot.append(repository.page(
                            requested,
                            snapshot.nextCursor())));
        } catch (FinanceException failure) {
            failed(operation, Optional.of(snapshot), failure);
        }
    }

    private void ready(long operation, FinanceSnapshot snapshot) {
        if (!current(operation)) {
            return;
        }
        busy.set(false);
        publish(FinanceListState.ready(
                snapshot,
                Optional.empty(),
                Optional.empty()));
    }

    private void failed(
            long operation,
            Optional<FinanceSnapshot> fallback,
            FinanceException failure) {
        if (!current(operation)) {
            return;
        }
        busy.set(false);
        if (fallback.isPresent()
                && switch (failure.kind()) {
                    case NETWORK, RATE_LIMITED, SERVICE_UNAVAILABLE -> true;
                    default -> false;
                }) {
            publish(FinanceListState.ready(
                    fallback.orElseThrow(),
                    Optional.of(failure.kind()),
                    failure.requestId()));
        } else {
            publish(FinanceListState.error(failure));
        }
    }

    private boolean current(long operation) {
        return generation.get() == operation
                && current.phase() != FinanceListState.Phase.CLOSED;
    }

    private void publish(FinanceListState state) {
        current = state;
        main.execute(() -> listeners.forEach(
                listener -> listener.onFinanceListStateChanged(state)));
    }
}
