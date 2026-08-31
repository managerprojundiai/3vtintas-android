package br.com.tresvtintas.mobile.core.order;

import java.util.Optional;

public record OrderListState(Phase phase, Optional<OrderSnapshot> snapshot,
        Optional<OrderFailureKind> failure, Optional<String> requestId) {
    public enum Phase { EMPTY, LOADING, READY, REFRESHING, LOADING_MORE, ERROR, CLOSED }

    public OrderListState {
        if (phase == null) {
            throw new IllegalArgumentException("Order list phase is required.");
        }
        snapshot = snapshot == null ? Optional.empty() : snapshot;
        failure = failure == null ? Optional.empty() : failure;
        requestId = requestId == null ? Optional.empty() : requestId;
        if ((phase == Phase.READY || phase == Phase.REFRESHING || phase == Phase.LOADING_MORE)
                && snapshot.isEmpty()) {
            throw new IllegalArgumentException("Order list phase requires a snapshot.");
        }
    }

    public static OrderListState empty() { return phase(Phase.EMPTY); }
    public static OrderListState loading() { return phase(Phase.LOADING); }
    public static OrderListState refreshing(OrderSnapshot value) { return snapshot(Phase.REFRESHING, value); }
    public static OrderListState loadingMore(OrderSnapshot value) { return snapshot(Phase.LOADING_MORE, value); }
    public static OrderListState ready(OrderSnapshot value, Optional<OrderFailureKind> warning,
            Optional<String> requestId) {
        return new OrderListState(Phase.READY, Optional.of(value), warning, requestId);
    }
    public static OrderListState error(OrderException value) {
        return new OrderListState(Phase.ERROR, Optional.empty(), Optional.of(value.kind()), value.requestId());
    }
    public static OrderListState closed() { return phase(Phase.CLOSED); }
    private static OrderListState phase(Phase value) {
        return new OrderListState(value, Optional.empty(), Optional.empty(), Optional.empty());
    }
    private static OrderListState snapshot(Phase phase, OrderSnapshot value) {
        return new OrderListState(phase, Optional.of(value), Optional.empty(), Optional.empty());
    }
}
