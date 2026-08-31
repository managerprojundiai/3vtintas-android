package br.com.tresvtintas.mobile.core.order;

import java.util.Optional;

public record OrderDetailState(Phase phase, Optional<OrderDetail> order,
        Optional<OrderFailureKind> failure, Optional<String> requestId) {
    public enum Phase { EMPTY, LOADING, READY, ERROR, CLOSED }
    public OrderDetailState {
        if (phase == null) {
            throw new IllegalArgumentException("Order detail phase is required.");
        }
        order = order == null ? Optional.empty() : order;
        failure = failure == null ? Optional.empty() : failure;
        requestId = requestId == null ? Optional.empty() : requestId;
        if (phase == Phase.READY && order.isEmpty()) {
            throw new IllegalArgumentException("Ready order detail requires an order.");
        }
    }
    public static OrderDetailState empty() { return phase(Phase.EMPTY); }
    public static OrderDetailState loading() { return phase(Phase.LOADING); }
    public static OrderDetailState ready(OrderDetail value) {
        return new OrderDetailState(Phase.READY, Optional.of(value), Optional.empty(), Optional.empty());
    }
    public static OrderDetailState error(OrderException value) {
        return new OrderDetailState(Phase.ERROR, Optional.empty(), Optional.of(value.kind()), value.requestId());
    }
    public static OrderDetailState closed() { return phase(Phase.CLOSED); }
    private static OrderDetailState phase(Phase value) {
        return new OrderDetailState(value, Optional.empty(), Optional.empty(), Optional.empty());
    }
}
