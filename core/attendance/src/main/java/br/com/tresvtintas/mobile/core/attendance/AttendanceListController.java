package br.com.tresvtintas.mobile.core.attendance;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class AttendanceListController {
    @FunctionalInterface
    public interface Listener {
        void onAttendanceListStateChanged(AttendanceListState state);
    }

    private final AttendanceRepository repository;
    private final Executor worker;
    private final Executor main;
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final AtomicLong generation = new AtomicLong();
    private volatile Optional<AttendanceQuery> activeQuery =
            Optional.empty();
    private volatile AttendanceListState current =
            AttendanceListState.empty();

    public AttendanceListController(
            AttendanceRepository repository,
            Executor worker,
            Executor main) {
        this.repository = Objects.requireNonNull(
                repository,
                "Attendance repository is required.");
        this.worker = Objects.requireNonNull(
                worker,
                "Attendance worker is required.");
        this.main = Objects.requireNonNull(
                main,
                "Attendance main executor is required.");
    }

    public void subscribe(Listener listener) {
        Listener required = Objects.requireNonNull(
                listener,
                "Attendance listener is required.");
        listeners.add(required);
        AttendanceListState snapshot = current;
        main.execute(() ->
                required.onAttendanceListStateChanged(snapshot));
    }

    public void unsubscribe(Listener listener) {
        listeners.remove(listener);
    }

    public Optional<AttendanceQuery> currentQuery() {
        return activeQuery;
    }

    public void open(AttendanceQuery query) {
        Objects.requireNonNull(query, "Attendance query is required.");
        activeQuery = Optional.of(query);
        busy.set(true);
        long operation = generation.incrementAndGet();
        publish(AttendanceListState.loading());
        worker.execute(() -> first(
                query,
                operation,
                Optional.empty()));
    }

    public void refresh() {
        Optional<AttendanceListState.Snapshot> snapshot =
                current.snapshot();
        if (snapshot.isEmpty()
                || !busy.compareAndSet(false, true)) {
            return;
        }
        long operation = generation.incrementAndGet();
        AttendanceListState.Snapshot fallback = snapshot.orElseThrow();
        publish(AttendanceListState.refreshing(fallback));
        worker.execute(() -> first(
                fallback.query(),
                operation,
                Optional.of(fallback)));
    }

    public void loadMore() {
        Optional<AttendanceListState.Snapshot> snapshot =
                current.snapshot();
        if (snapshot.isEmpty()
                || !snapshot.orElseThrow().hasMore()
                || !busy.compareAndSet(false, true)) {
            return;
        }
        long operation = generation.incrementAndGet();
        AttendanceListState.Snapshot value = snapshot.orElseThrow();
        publish(AttendanceListState.loadingMore(value));
        worker.execute(() -> next(operation, value));
    }

    public void close() {
        generation.incrementAndGet();
        busy.set(false);
        activeQuery = Optional.empty();
        publish(AttendanceListState.closed());
        listeners.clear();
    }

    private void first(
            AttendanceQuery query,
            long operation,
            Optional<AttendanceListState.Snapshot> fallback) {
        try {
            AttendancePage page = repository.page(
                    query,
                    Optional.empty());
            ready(operation, new AttendanceListState.Snapshot(
                    query,
                    page.items(),
                    page.nextCursor()));
        } catch (AttendanceException failure) {
            failed(operation, fallback, failure);
        }
    }

    private void next(
            long operation,
            AttendanceListState.Snapshot snapshot) {
        try {
            ready(operation, snapshot.append(repository.page(
                    snapshot.query(),
                    snapshot.nextCursor())));
        } catch (AttendanceException failure) {
            failed(operation, Optional.of(snapshot), failure);
        }
    }

    private void ready(
            long operation,
            AttendanceListState.Snapshot snapshot) {
        if (!isCurrent(operation)) {
            return;
        }
        busy.set(false);
        publish(AttendanceListState.ready(snapshot));
    }

    private void failed(
            long operation,
            Optional<AttendanceListState.Snapshot> fallback,
            AttendanceException failure) {
        if (!isCurrent(operation)) {
            return;
        }
        busy.set(false);
        if (fallback.isPresent() && isTransient(failure.kind())) {
            publish(AttendanceListState.warning(
                    fallback.orElseThrow(),
                    failure));
        } else {
            publish(AttendanceListState.error(failure));
        }
    }

    private boolean isCurrent(long operation) {
        return generation.get() == operation
                && current.phase() != AttendanceListState.Phase.CLOSED;
    }

    private static boolean isTransient(AttendanceFailureKind failure) {
        return failure == AttendanceFailureKind.NETWORK
                || failure == AttendanceFailureKind.RATE_LIMITED
                || failure == AttendanceFailureKind.SERVICE_UNAVAILABLE;
    }

    private void publish(AttendanceListState state) {
        current = state;
        main.execute(() -> listeners.forEach(listener ->
                listener.onAttendanceListStateChanged(state)));
    }
}
