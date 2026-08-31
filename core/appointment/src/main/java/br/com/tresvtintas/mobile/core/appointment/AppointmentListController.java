package br.com.tresvtintas.mobile.core.appointment;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class AppointmentListController {
    private final AppointmentRepository repository;
    private final Executor worker;
    private final Executor main;
    private final Set<AppointmentListStateListener> listeners =
            new CopyOnWriteArraySet<>();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final AtomicLong generation = new AtomicLong();
    private volatile AppointmentListState current =
            AppointmentListState.empty();
    private volatile AppointmentQuery query;

    public AppointmentListController(
            AppointmentRepository repository,
            AppointmentQuery initialQuery,
            Executor worker,
            Executor main) {
        this.repository = Objects.requireNonNull(
                repository,
                "Appointment repository is required.");
        this.query = Objects.requireNonNull(
                initialQuery,
                "Appointment query is required.");
        this.worker = Objects.requireNonNull(
                worker,
                "Appointment worker is required.");
        this.main = Objects.requireNonNull(main, "Main executor is required.");
    }

    public void subscribe(AppointmentListStateListener listener) {
        AppointmentListStateListener required = Objects.requireNonNull(
                listener,
                "Appointment listener is required.");
        listeners.add(required);
        AppointmentListState snapshot = current;
        main.execute(() -> required.onAppointmentListStateChanged(snapshot));
    }

    public void unsubscribe(AppointmentListStateListener listener) {
        listeners.remove(listener);
    }

    public AppointmentQuery currentQuery() {
        return query;
    }

    public void open(AppointmentQuery requested) {
        query = Objects.requireNonNull(
                requested,
                "Appointment query is required.");
        long operation = generation.incrementAndGet();
        busy.set(true);
        publish(AppointmentListState.loading());
        worker.execute(() -> first(requested, operation, Optional.empty()));
    }

    public void refresh() {
        if (current.phase() == AppointmentListState.Phase.CLOSED
                || !busy.compareAndSet(false, true)) {
            return;
        }
        long operation = generation.incrementAndGet();
        Optional<AppointmentSnapshot> fallback = current.snapshot();
        fallback.ifPresentOrElse(
                value -> publish(AppointmentListState.refreshing(value)),
                () -> publish(AppointmentListState.loading()));
        worker.execute(() -> first(query, operation, fallback));
    }

    public void loadMore() {
        Optional<AppointmentSnapshot> snapshot = current.snapshot();
        if (snapshot.isEmpty()
                || !snapshot.orElseThrow().hasMore()
                || !busy.compareAndSet(false, true)) {
            return;
        }
        long operation = generation.incrementAndGet();
        AppointmentSnapshot value = snapshot.orElseThrow();
        publish(AppointmentListState.loadingMore(value));
        worker.execute(() -> next(query, operation, value));
    }

    public void close() {
        generation.incrementAndGet();
        busy.set(false);
        publish(AppointmentListState.closed());
        listeners.clear();
    }

    private void first(
            AppointmentQuery requested,
            long operation,
            Optional<AppointmentSnapshot> fallback) {
        try {
            ready(
                    operation,
                    AppointmentSnapshot.from(repository.page(
                            requested,
                            Optional.empty())));
        } catch (AppointmentException failure) {
            failed(operation, fallback, failure);
        }
    }

    private void next(
            AppointmentQuery requested,
            long operation,
            AppointmentSnapshot snapshot) {
        try {
            ready(
                    operation,
                    snapshot.append(repository.page(
                            requested,
                            snapshot.nextCursor())));
        } catch (AppointmentException failure) {
            failed(operation, Optional.of(snapshot), failure);
        }
    }

    private void ready(long operation, AppointmentSnapshot snapshot) {
        if (!current(operation)) {
            return;
        }
        busy.set(false);
        publish(AppointmentListState.ready(
                snapshot,
                Optional.empty(),
                Optional.empty()));
    }

    private void failed(
            long operation,
            Optional<AppointmentSnapshot> fallback,
            AppointmentException failure) {
        if (!current(operation)) {
            return;
        }
        busy.set(false);
        if (fallback.isPresent()
                && switch (failure.kind()) {
                    case NETWORK, RATE_LIMITED, SERVICE_UNAVAILABLE -> true;
                    default -> false;
                }) {
            publish(AppointmentListState.ready(
                    fallback.orElseThrow(),
                    Optional.of(failure.kind()),
                    failure.requestId()));
        } else {
            publish(AppointmentListState.error(failure));
        }
    }

    private boolean current(long operation) {
        return generation.get() == operation
                && current.phase() != AppointmentListState.Phase.CLOSED;
    }

    private void publish(AppointmentListState state) {
        current = state;
        main.execute(() -> listeners.forEach(
                listener -> listener.onAppointmentListStateChanged(state)));
    }
}
