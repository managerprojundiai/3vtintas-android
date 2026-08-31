package br.com.tresvtintas.mobile.core.appointment;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class AppointmentResponsibleController {
    private final AppointmentRepository repository;
    private final Executor worker;
    private final Executor main;
    private final Set<AppointmentResponsibleStateListener> listeners =
            new CopyOnWriteArraySet<>();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final AtomicLong generation = new AtomicLong();
    private volatile AppointmentResponsibleState current =
            AppointmentResponsibleState.empty();
    private volatile AppointmentResponsibleQuery query;

    public AppointmentResponsibleController(
            AppointmentRepository repository,
            Executor worker,
            Executor main) {
        this.repository = Objects.requireNonNull(
                repository,
                "Appointment repository is required.");
        this.worker = Objects.requireNonNull(
                worker,
                "Appointment worker is required.");
        this.main = Objects.requireNonNull(main, "Main executor is required.");
    }

    public void subscribe(AppointmentResponsibleStateListener listener) {
        AppointmentResponsibleStateListener required = Objects.requireNonNull(
                listener,
                "Appointment responsible listener is required.");
        listeners.add(required);
        AppointmentResponsibleState snapshot = current;
        main.execute(() ->
                required.onAppointmentResponsibleStateChanged(snapshot));
    }

    public void unsubscribe(AppointmentResponsibleStateListener listener) {
        listeners.remove(listener);
    }

    public void open(AppointmentResponsibleQuery requested) {
        query = Objects.requireNonNull(
                requested,
                "Appointment responsible query is required.");
        busy.set(true);
        long operation = generation.incrementAndGet();
        publish(AppointmentResponsibleState.loading());
        worker.execute(() -> load(requested, Optional.empty(), operation, null));
    }

    public void loadMore() {
        Optional<AppointmentResponsibleSnapshot> snapshot = current.snapshot();
        if (snapshot.isEmpty()
                || snapshot.orElseThrow().nextCursor().isEmpty()
                || !busy.compareAndSet(false, true)) {
            return;
        }
        AppointmentResponsibleSnapshot currentSnapshot =
                snapshot.orElseThrow();
        long operation = generation.incrementAndGet();
        publish(AppointmentResponsibleState.loadingMore(currentSnapshot));
        worker.execute(() -> load(
                query,
                currentSnapshot.nextCursor(),
                operation,
                currentSnapshot));
    }

    public void close() {
        generation.incrementAndGet();
        busy.set(false);
        publish(AppointmentResponsibleState.closed());
        listeners.clear();
    }

    private void load(
            AppointmentResponsibleQuery requested,
            Optional<String> cursor,
            long operation,
            AppointmentResponsibleSnapshot previous) {
        try {
            AppointmentResponsiblePage page =
                    repository.responsibles(requested, cursor);
            if (!current(operation)) {
                return;
            }
            busy.set(false);
            publish(AppointmentResponsibleState.ready(
                    previous == null
                            ? AppointmentResponsibleSnapshot.from(page)
                            : previous.append(page)));
        } catch (AppointmentException failure) {
            if (!current(operation)) {
                return;
            }
            busy.set(false);
            publish(AppointmentResponsibleState.error(failure));
        }
    }

    private boolean current(long operation) {
        return generation.get() == operation
                && current.phase() != AppointmentResponsibleState.Phase.CLOSED;
    }

    private void publish(AppointmentResponsibleState state) {
        current = state;
        main.execute(() -> listeners.forEach(
                listener ->
                        listener.onAppointmentResponsibleStateChanged(state)));
    }
}
