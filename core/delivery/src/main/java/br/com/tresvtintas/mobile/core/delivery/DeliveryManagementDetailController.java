package br.com.tresvtintas.mobile.core.delivery;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

public final class DeliveryManagementDetailController {
    @FunctionalInterface
    public interface Listener {
        void onDeliveryManagementDetailStateChanged(
                DeliveryManagementDetailState state);
    }

    private final DeliveryManagementRepository repository;
    private final Executor worker;
    private final Executor main;
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private final AtomicLong generation = new AtomicLong();
    private volatile DeliveryManagementDetailState current =
            DeliveryManagementDetailState.empty();

    public DeliveryManagementDetailController(
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
        DeliveryManagementDetailState snapshot = current;
        main.execute(() ->
                required.onDeliveryManagementDetailStateChanged(snapshot));
    }

    public void unsubscribe(Listener listener) {
        listeners.remove(listener);
    }

    public void load(long organizationId, long orderId) {
        if (organizationId < 1 || orderId < 1) {
            publish(DeliveryManagementDetailState.error(
                    new DeliveryException(
                            DeliveryFailureKind.INVALID_REQUEST,
                            "Management delivery identifiers are invalid.")));
            return;
        }
        long operation = generation.incrementAndGet();
        publish(DeliveryManagementDetailState.loading());
        worker.execute(() -> execute(
                organizationId,
                orderId,
                operation));
    }

    public void close() {
        generation.incrementAndGet();
        publish(DeliveryManagementDetailState.closed());
        listeners.clear();
    }

    private void execute(
            long organizationId,
            long orderId,
            long operation) {
        DeliveryManagementDetailState state;
        try {
            state = DeliveryManagementDetailState.ready(
                    repository.detail(organizationId, orderId),
                    repository.drivers(organizationId));
        } catch (DeliveryException failure) {
            state = DeliveryManagementDetailState.error(failure);
        }
        if (generation.get() == operation
                && current.phase()
                        != DeliveryManagementDetailState.Phase.CLOSED) {
            publish(state);
        }
    }

    private void publish(DeliveryManagementDetailState state) {
        current = state;
        main.execute(() -> listeners.forEach(listener ->
                listener.onDeliveryManagementDetailStateChanged(state)));
    }
}
