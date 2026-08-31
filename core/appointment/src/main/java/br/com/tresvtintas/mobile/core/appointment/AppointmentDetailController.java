package br.com.tresvtintas.mobile.core.appointment;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

public final class AppointmentDetailController {
    private final AppointmentRepository repository;
    private final Executor worker;
    private final Executor main;
    private final Set<AppointmentDetailStateListener> listeners =
            new CopyOnWriteArraySet<>();
    private final AtomicLong generation = new AtomicLong();
    private volatile AppointmentDetailState current =
            AppointmentDetailState.empty();

    public AppointmentDetailController(
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

    public void subscribe(AppointmentDetailStateListener listener) {
        AppointmentDetailStateListener required = Objects.requireNonNull(
                listener,
                "Appointment detail listener is required.");
        listeners.add(required);
        AppointmentDetailState snapshot = current;
        main.execute(() -> required.onAppointmentDetailStateChanged(snapshot));
    }

    public void unsubscribe(AppointmentDetailStateListener listener) {
        listeners.remove(listener);
    }

    public void load(long appointmentId, AppointmentScope scope) {
        if (appointmentId < 1 || scope == null) {
            publish(AppointmentDetailState.error(new AppointmentException(
                    AppointmentFailureKind.INVALID_REQUEST,
                    "Appointment detail input is invalid.")));
            return;
        }
        long operation = generation.incrementAndGet();
        publish(AppointmentDetailState.loading());
        worker.execute(() -> {
            try {
                AppointmentDetail detail =
                        repository.detail(appointmentId, scope);
                if (current(operation)) {
                    publish(AppointmentDetailState.ready(detail));
                }
            } catch (AppointmentException failure) {
                if (current(operation)) {
                    publish(AppointmentDetailState.error(failure));
                }
            }
        });
    }

    public void close() {
        generation.incrementAndGet();
        publish(AppointmentDetailState.closed());
        listeners.clear();
    }

    private boolean current(long operation) {
        return generation.get() == operation
                && current.phase() != AppointmentDetailState.Phase.CLOSED;
    }

    private void publish(AppointmentDetailState state) {
        current = state;
        main.execute(() -> listeners.forEach(
                listener -> listener.onAppointmentDetailStateChanged(state)));
    }
}
