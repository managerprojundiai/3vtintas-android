package br.com.tresvtintas.mobile.core.delivery;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class DeliveryRouteController {
    @FunctionalInterface
    public interface Listener {
        void onDeliveryRouteStateChanged(DeliveryRouteState state);
    }

    private final DeliveryRouteRepository repository;
    private final Executor worker;
    private final Executor main;
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final AtomicLong generation = new AtomicLong();
    private volatile DeliveryRouteState current;

    public DeliveryRouteController(
            DeliveryRouteRepository repository,
            Executor worker,
            Executor main,
            LocalDate initialDate) {
        this.repository = Objects.requireNonNull(repository, "Route repository is required.");
        this.worker = Objects.requireNonNull(worker, "Route worker is required.");
        this.main = Objects.requireNonNull(main, "Route main executor is required.");
        current = DeliveryRouteState.empty(Objects.requireNonNull(
                initialDate, "Initial route date is required."));
    }

    public void subscribe(Listener listener) {
        Listener required = Objects.requireNonNull(listener, "Route listener is required.");
        listeners.add(required);
        DeliveryRouteState snapshot = current;
        main.execute(() -> required.onDeliveryRouteStateChanged(snapshot));
    }

    public void unsubscribe(Listener listener) {
        listeners.remove(listener);
    }

    public void open(LocalDate date) {
        LocalDate requested = Objects.requireNonNull(date, "Route date is required.");
        long operation = generation.incrementAndGet();
        busy.set(true);
        publish(DeliveryRouteState.loading(requested));
        worker.execute(() -> load(requested, operation));
    }

    public void refresh() {
        if (current.phase() == DeliveryRouteState.Phase.CLOSED
                || !busy.compareAndSet(false, true)) {
            return;
        }
        long operation = generation.incrementAndGet();
        LocalDate date = current.serviceDate();
        current.page().ifPresentOrElse(
                page -> publish(DeliveryRouteState.refreshing(date, page)),
                () -> publish(DeliveryRouteState.loading(date)));
        worker.execute(() -> load(date, operation));
    }

    public void close() {
        generation.incrementAndGet();
        busy.set(false);
        publish(DeliveryRouteState.closed(current.serviceDate()));
        listeners.clear();
    }

    private void load(LocalDate date, long operation) {
        DeliveryRouteState result;
        try {
            result = DeliveryRouteState.ready(date, repository.routes(date));
        } catch (DeliveryException failure) {
            result = DeliveryRouteState.error(date, failure);
        }
        if (generation.get() == operation
                && current.phase() != DeliveryRouteState.Phase.CLOSED) {
            busy.set(false);
            publish(result);
        }
    }

    private void publish(DeliveryRouteState state) {
        current = state;
        main.execute(() -> listeners.forEach(
                listener -> listener.onDeliveryRouteStateChanged(state)));
    }
}
