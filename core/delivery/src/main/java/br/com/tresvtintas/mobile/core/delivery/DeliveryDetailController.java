package br.com.tresvtintas.mobile.core.delivery;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

public final class DeliveryDetailController {
    private static final long MIN_DELIVERY_ID = 1L;

    @FunctionalInterface
    public interface Listener {
        void onDeliveryDetailStateChanged(DeliveryDetailState state);
    }

    private final DeliveryRepository repository;
    private final Executor worker;
    private final Executor main;
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private final AtomicLong generation = new AtomicLong();
    private volatile DeliveryDetailState current = DeliveryDetailState.empty();

    public DeliveryDetailController(
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
        DeliveryDetailState snapshot = current;
        main.execute(() -> required.onDeliveryDetailStateChanged(snapshot));
    }

    public void unsubscribe(Listener listener) {
        listeners.remove(listener);
    }

    public void load(long deliveryId) {
        if (deliveryId < MIN_DELIVERY_ID) {
            publish(DeliveryDetailState.error(new DeliveryException(
                    DeliveryFailureKind.INVALID_REQUEST,
                    "Delivery ID is invalid.")));
            return;
        }
        long operation = generation.incrementAndGet();
        publish(DeliveryDetailState.loading());
        worker.execute(() -> execute(deliveryId, operation));
    }

    public void close() {
        generation.incrementAndGet();
        publish(DeliveryDetailState.closed());
        listeners.clear();
    }

    private void execute(long deliveryId, long operation) {
        DeliveryDetailState state;
        try {
            state = DeliveryDetailState.ready(repository.detail(deliveryId));
        } catch (DeliveryException failure) {
            state = DeliveryDetailState.error(failure);
        }
        if (generation.get() == operation
                && current.phase() != DeliveryDetailState.Phase.CLOSED) {
            publish(state);
        }
    }

    private void publish(DeliveryDetailState state) {
        current = state;
        main.execute(() -> listeners.forEach(
                listener -> listener.onDeliveryDetailStateChanged(state)));
    }
}
