package br.com.tresvtintas.mobile.core.delivery;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class DeliveryActionController {
    @FunctionalInterface
    public interface Listener {
        void onDeliveryActionStateChanged(DeliveryActionState state);
    }

    private final DeliveryRepository repository;
    private final Executor worker;
    private final Executor main;
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final AtomicLong generation = new AtomicLong();
    private volatile DeliveryActionState current = DeliveryActionState.idle();

    public DeliveryActionController(
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
        DeliveryActionState snapshot = current;
        main.execute(() -> required.onDeliveryActionStateChanged(snapshot));
    }

    public void unsubscribe(Listener listener) {
        listeners.remove(listener);
    }

    public void execute(
            DeliveryAction action,
            long deliveryId,
            int expectedOrderRevision,
            String idempotencyKey) {
        if (action == null
                || deliveryId < 1
                || expectedOrderRevision < 1
                || idempotencyKey == null
                || idempotencyKey.length() < 16
                || idempotencyKey.length() > 255
                || !idempotencyKey.matches("^[\\x21-\\x7e]+$")) {
            DeliveryAction safeAction =
                    action == null ? DeliveryAction.START : action;
            publish(DeliveryActionState.error(safeAction, new DeliveryException(
                    DeliveryFailureKind.INVALID_REQUEST,
                    "Delivery action request is invalid.")));
            return;
        }
        if (!busy.compareAndSet(false, true)) {
            return;
        }
        long operation = generation.incrementAndGet();
        publish(DeliveryActionState.running(action));
        worker.execute(() -> run(
                operation,
                action,
                deliveryId,
                expectedOrderRevision,
                idempotencyKey));
    }

    public void close() {
        generation.incrementAndGet();
        busy.set(false);
        publish(DeliveryActionState.closed());
        listeners.clear();
    }

    private void run(
            long operation,
            DeliveryAction action,
            long deliveryId,
            int expectedOrderRevision,
            String idempotencyKey) {
        DeliveryActionState state;
        try {
            DeliveryMutationResult result = action == DeliveryAction.START
                    ? repository.start(
                            deliveryId,
                            expectedOrderRevision,
                            idempotencyKey)
                    : repository.complete(
                            deliveryId,
                            expectedOrderRevision,
                            idempotencyKey);
            state = DeliveryActionState.success(result);
        } catch (DeliveryException failure) {
            state = DeliveryActionState.error(action, failure);
        }
        if (generation.get() == operation
                && current.phase() != DeliveryActionState.Phase.CLOSED) {
            busy.set(false);
            publish(state);
        }
    }

    private void publish(DeliveryActionState state) {
        current = state;
        main.execute(() -> listeners.forEach(
                listener -> listener.onDeliveryActionStateChanged(state)));
    }
}
