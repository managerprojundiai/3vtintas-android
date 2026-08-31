package br.com.tresvtintas.mobile.core.order;

import java.util.Optional;

public record OrderActionState(
        Phase phase,
        Optional<OrderAction> action,
        Optional<OrderActionResult> result,
        Optional<OrderFailureKind> failure,
        Optional<String> requestId) {
    public enum Phase {
        IDLE,
        RUNNING,
        SUCCESS,
        ERROR,
        CLOSED
    }

    public OrderActionState {
        if (phase == null) {
            throw new IllegalArgumentException("Order action phase is required.");
        }
        action = action == null ? Optional.empty() : action;
        result = result == null ? Optional.empty() : result;
        failure = failure == null ? Optional.empty() : failure;
        requestId = requestId == null ? Optional.empty() : requestId;
        if (phase == Phase.RUNNING && action.isEmpty()) {
            throw new IllegalArgumentException(
                    "A running order action requires its action.");
        }
        if (phase == Phase.SUCCESS
                && (result.isEmpty() || action.isEmpty())) {
            throw new IllegalArgumentException(
                    "A successful order action requires its result.");
        }
        if (phase == Phase.ERROR
                && (failure.isEmpty() || action.isEmpty())) {
            throw new IllegalArgumentException(
                    "A failed order action requires its failure.");
        }
    }

    public static OrderActionState idle() {
        return phase(Phase.IDLE);
    }

    public static OrderActionState running(OrderAction action) {
        return new OrderActionState(
                Phase.RUNNING,
                Optional.of(action),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    public static OrderActionState success(OrderActionResult result) {
        return new OrderActionState(
                Phase.SUCCESS,
                Optional.of(result.action()),
                Optional.of(result),
                Optional.empty(),
                Optional.empty());
    }

    public static OrderActionState error(
            OrderAction action,
            OrderException failure) {
        return new OrderActionState(
                Phase.ERROR,
                Optional.of(action),
                Optional.empty(),
                Optional.of(failure.kind()),
                failure.requestId());
    }

    public static OrderActionState closed() {
        return phase(Phase.CLOSED);
    }

    private static OrderActionState phase(Phase phase) {
        return new OrderActionState(
                phase,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }
}
