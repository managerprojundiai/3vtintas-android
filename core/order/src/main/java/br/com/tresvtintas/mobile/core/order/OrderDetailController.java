package br.com.tresvtintas.mobile.core.order;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

public final class OrderDetailController {
    private static final long MIN_ORDER_ID = 1L;

    private final OrderRepository repository;
    private final Executor worker;
    private final Executor main;
    private final Set<OrderDetailStateListener> listeners = new CopyOnWriteArraySet<>();
    private final AtomicLong generation = new AtomicLong();
    private volatile OrderDetailState current = OrderDetailState.empty();

    public OrderDetailController(OrderRepository repository, Executor worker, Executor main) {
        this.repository = Objects.requireNonNull(repository, "Order repository is required.");
        this.worker = Objects.requireNonNull(worker, "Order worker is required.");
        this.main = Objects.requireNonNull(main, "Main executor is required.");
    }
    public void subscribe(OrderDetailStateListener listener) {
        OrderDetailStateListener required = Objects.requireNonNull(listener, "Order listener is required.");
        listeners.add(required);
        OrderDetailState snapshot = current;
        main.execute(() -> required.onOrderDetailStateChanged(snapshot));
    }
    public void unsubscribe(OrderDetailStateListener listener) { listeners.remove(listener); }
    public void load(long orderId) {
        if (orderId < MIN_ORDER_ID) {
            publish(OrderDetailState.error(new OrderException(OrderFailureKind.INVALID_REQUEST,
                    "Order ID is invalid.")));
            return;
        }
        long operation = generation.incrementAndGet();
        publish(OrderDetailState.loading());
        worker.execute(() -> execute(orderId, operation));
    }
    public void close() {
        generation.incrementAndGet();
        publish(OrderDetailState.closed());
        listeners.clear();
    }
    private void execute(long orderId, long operation) {
        try { complete(operation, OrderDetailState.ready(repository.detail(orderId))); }
        catch (OrderException failure) { complete(operation, OrderDetailState.error(failure)); }
    }
    private void complete(long operation, OrderDetailState state) {
        if (generation.get() == operation && current.phase() != OrderDetailState.Phase.CLOSED) {
            publish(state);
        }
    }
    private void publish(OrderDetailState state) {
        current = state;
        main.execute(() -> listeners.forEach(listener -> listener.onOrderDetailStateChanged(state)));
    }
}
