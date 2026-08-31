package br.com.tresvtintas.mobile.core.order;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class OrderListController {
    private final OrderRepository repository;
    private final Executor worker;
    private final Executor main;
    private final Set<OrderListStateListener> listeners = new CopyOnWriteArraySet<>();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final AtomicLong generation = new AtomicLong();
    private volatile OrderListState current = OrderListState.empty();
    private volatile OrderQuery query = OrderQuery.initial();

    public OrderListController(OrderRepository repository, Executor worker, Executor main) {
        this.repository = Objects.requireNonNull(repository, "Order repository is required.");
        this.worker = Objects.requireNonNull(worker, "Order worker is required.");
        this.main = Objects.requireNonNull(main, "Main executor is required.");
    }
    public void subscribe(OrderListStateListener listener) {
        OrderListStateListener required = Objects.requireNonNull(listener, "Order listener is required.");
        listeners.add(required);
        OrderListState snapshot = current;
        main.execute(() -> required.onOrderListStateChanged(snapshot));
    }
    public void unsubscribe(OrderListStateListener listener) { listeners.remove(listener); }
    public OrderQuery currentQuery() { return query; }
    public void open(OrderQuery requested) {
        query = Objects.requireNonNull(requested, "Order query is required.");
        long operation = generation.incrementAndGet();
        busy.set(true);
        publish(OrderListState.loading());
        worker.execute(() -> first(requested, operation, Optional.empty()));
    }
    public void refresh() {
        if (current.phase() == OrderListState.Phase.CLOSED || !busy.compareAndSet(false, true)) {
            return;
        }
        long operation = generation.incrementAndGet();
        Optional<OrderSnapshot> fallback = current.snapshot();
        fallback.ifPresentOrElse(value -> publish(OrderListState.refreshing(value)),
                () -> publish(OrderListState.loading()));
        worker.execute(() -> first(query, operation, fallback));
    }
    public void loadMore() {
        Optional<OrderSnapshot> snapshot = current.snapshot();
        if (snapshot.isEmpty() || !snapshot.orElseThrow().hasMore()
                || !busy.compareAndSet(false, true)) {
            return;
        }
        long operation = generation.incrementAndGet();
        OrderSnapshot value = snapshot.orElseThrow();
        publish(OrderListState.loadingMore(value));
        worker.execute(() -> next(query, operation, value));
    }
    public void close() {
        generation.incrementAndGet();
        busy.set(false);
        publish(OrderListState.closed());
        listeners.clear();
    }
    private void first(OrderQuery requested, long operation, Optional<OrderSnapshot> fallback) {
        try { ready(operation, OrderSnapshot.from(repository.page(requested, Optional.empty()))); }
        catch (OrderException failure) { failed(operation, fallback, failure); }
    }
    private void next(OrderQuery requested, long operation, OrderSnapshot snapshot) {
        try { ready(operation, snapshot.append(repository.page(requested, snapshot.nextCursor()))); }
        catch (OrderException failure) { failed(operation, Optional.of(snapshot), failure); }
    }
    private void ready(long operation, OrderSnapshot snapshot) {
        if (!current(operation)) {
            return;
        }
        busy.set(false);
        publish(OrderListState.ready(snapshot, Optional.empty(), Optional.empty()));
    }
    private void failed(long operation, Optional<OrderSnapshot> fallback, OrderException failure) {
        if (!current(operation)) {
            return;
        }
        busy.set(false);
        if (fallback.isPresent() && switch (failure.kind()) {
            case NETWORK, RATE_LIMITED, SERVICE_UNAVAILABLE -> true;
            default -> false;
        }) {
            publish(OrderListState.ready(
                    fallback.orElseThrow(),
                    Optional.of(failure.kind()),
                    failure.requestId()));
        } else {
            publish(OrderListState.error(failure));
        }
    }
    private boolean current(long operation) {
        return generation.get() == operation && current.phase() != OrderListState.Phase.CLOSED;
    }
    private void publish(OrderListState state) {
        current = state;
        main.execute(() -> listeners.forEach(listener -> listener.onOrderListStateChanged(state)));
    }
}
