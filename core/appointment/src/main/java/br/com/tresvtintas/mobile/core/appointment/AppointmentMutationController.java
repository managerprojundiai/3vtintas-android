package br.com.tresvtintas.mobile.core.appointment;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class AppointmentMutationController {
    private final AppointmentRepository repository;
    private final Executor worker;
    private final Executor main;
    private final Set<AppointmentMutationStateListener> listeners =
            new CopyOnWriteArraySet<>();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final AtomicLong generation = new AtomicLong();
    private volatile AppointmentMutationState current =
            AppointmentMutationState.idle();

    public AppointmentMutationController(
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

    public void subscribe(AppointmentMutationStateListener listener) {
        AppointmentMutationStateListener required = Objects.requireNonNull(
                listener,
                "Appointment mutation listener is required.");
        listeners.add(required);
        AppointmentMutationState snapshot = current;
        main.execute(() -> required.onAppointmentMutationStateChanged(snapshot));
    }

    public void unsubscribe(AppointmentMutationStateListener listener) {
        listeners.remove(listener);
    }

    public void create(AppointmentDraft draft, String idempotencyKey) {
        if (draft == null || !validKey(idempotencyKey)) {
            invalid(AppointmentMutationAction.CREATE);
            return;
        }
        execute(
                AppointmentMutationAction.CREATE,
                () -> repository.create(draft, idempotencyKey));
    }

    public void update(
            long appointmentId,
            AppointmentEdit edit,
            String idempotencyKey) {
        if (appointmentId < 1
                || edit == null
                || !validKey(idempotencyKey)) {
            invalid(AppointmentMutationAction.UPDATE);
            return;
        }
        execute(
                AppointmentMutationAction.UPDATE,
                () -> repository.update(
                        appointmentId,
                        edit,
                        idempotencyKey));
    }

    public void transition(
            long appointmentId,
            AppointmentScope scope,
            long expectedRevision,
            AppointmentStatus status,
            String idempotencyKey) {
        if (appointmentId < 1
                || scope == null
                || expectedRevision < 1
                || status == null
                || status == AppointmentStatus.SCHEDULED
                || !validKey(idempotencyKey)) {
            invalid(AppointmentMutationAction.TRANSITION);
            return;
        }
        execute(
                AppointmentMutationAction.TRANSITION,
                () -> repository.transition(
                        appointmentId,
                        scope,
                        expectedRevision,
                        status,
                        idempotencyKey));
    }

    public void close() {
        generation.incrementAndGet();
        busy.set(false);
        publish(AppointmentMutationState.closed());
        listeners.clear();
    }

    private void execute(
            AppointmentMutationAction action,
            Operation operation) {
        if (!busy.compareAndSet(false, true)) {
            return;
        }
        long operationGeneration = generation.incrementAndGet();
        publish(AppointmentMutationState.running(action));
        worker.execute(() -> {
            try {
                complete(
                        operationGeneration,
                        AppointmentMutationState.success(
                                action,
                                operation.run()));
            } catch (AppointmentException failure) {
                complete(
                        operationGeneration,
                        AppointmentMutationState.error(action, failure));
            }
        });
    }

    private void complete(
            long operationGeneration,
            AppointmentMutationState state) {
        if (generation.get() != operationGeneration
                || current.phase() == AppointmentMutationState.Phase.CLOSED) {
            return;
        }
        busy.set(false);
        publish(state);
    }

    private void invalid(AppointmentMutationAction action) {
        publish(AppointmentMutationState.error(
                action,
                new AppointmentException(
                        AppointmentFailureKind.INVALID_REQUEST,
                        "Appointment mutation request is invalid.")));
    }

    private void publish(AppointmentMutationState state) {
        current = state;
        main.execute(() -> listeners.forEach(
                listener -> listener.onAppointmentMutationStateChanged(state)));
    }

    private static boolean validKey(String value) {
        return value != null
                && value.length() >= 16
                && value.length() <= 255
                && value.matches("^[\\x21-\\x7e]+$");
    }

    @FunctionalInterface
    private interface Operation {
        AppointmentMutationResult run() throws AppointmentException;
    }
}
