package br.com.tresvtintas.mobile.core.delivery;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class DeliveryManagementActionController {
    @FunctionalInterface
    public interface Listener {
        void onDeliveryManagementActionStateChanged(
                DeliveryManagementActionState state);
    }

    private final DeliveryManagementRepository repository;
    private final Executor worker;
    private final Executor main;
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final AtomicLong generation = new AtomicLong();
    private volatile DeliveryManagementActionState current =
            DeliveryManagementActionState.idle();

    public DeliveryManagementActionController(
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
        DeliveryManagementActionState snapshot = current;
        main.execute(() ->
                required.onDeliveryManagementActionStateChanged(snapshot));
    }

    public void unsubscribe(Listener listener) {
        listeners.remove(listener);
    }

    public void execute(
            DeliveryManagementCommand command,
            String idempotencyKey) {
        if (command == null || !validKey(idempotencyKey)) {
            DeliveryManagementAction action = command == null
                    ? DeliveryManagementAction.SCHEDULE
                    : command.action();
            publish(DeliveryManagementActionState.error(
                    action,
                    new DeliveryException(
                            DeliveryFailureKind.INVALID_REQUEST,
                            "Management delivery command is invalid.")));
            return;
        }
        if (!busy.compareAndSet(false, true)) {
            return;
        }
        long operation = generation.incrementAndGet();
        publish(DeliveryManagementActionState.running(command.action()));
        worker.execute(() -> run(operation, command, idempotencyKey));
    }

    public void close() {
        generation.incrementAndGet();
        busy.set(false);
        publish(DeliveryManagementActionState.closed());
        listeners.clear();
    }

    private void run(
            long operation,
            DeliveryManagementCommand command,
            String key) {
        DeliveryManagementActionState state;
        try {
            DeliveryManagementMutationResult result;
            if (command instanceof DeliveryManagementCommand.Schedule value) {
                result = repository.schedule(
                        value.organizationId(),
                        value.orderId(),
                        value.expectedOrderRevision(),
                        value.scheduledAt(),
                        value.durationMinutes(),
                        key);
            } else if (command
                    instanceof DeliveryManagementCommand.Assignment value) {
                result = repository.assign(
                        value.organizationId(),
                        value.orderId(),
                        value.expectedOrderRevision(),
                        value.driverUserId(),
                        key);
            } else {
                DeliveryManagementCommand.Completion value =
                        (DeliveryManagementCommand.Completion) command;
                result = repository.complete(
                        value.organizationId(),
                        value.orderId(),
                        value.expectedOrderRevision(),
                        key);
            }
            state = DeliveryManagementActionState.success(result);
        } catch (DeliveryException failure) {
            state = DeliveryManagementActionState.error(
                    command.action(),
                    failure);
        }
        if (generation.get() == operation
                && current.phase()
                        != DeliveryManagementActionState.Phase.CLOSED) {
            busy.set(false);
            publish(state);
        }
    }

    private static boolean validKey(String value) {
        return value != null
                && value.length() >= 16
                && value.length() <= 255
                && value.matches("^[\\x21-\\x7e]+$");
    }

    private void publish(DeliveryManagementActionState state) {
        current = state;
        main.execute(() -> listeners.forEach(listener ->
                listener.onDeliveryManagementActionStateChanged(state)));
    }
}
