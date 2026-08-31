package br.com.tresvtintas.mobile.core.delivery;

import java.util.Objects;
import java.util.Optional;

public record DeliveryActionState(
        Phase phase,
        Optional<DeliveryAction> action,
        Optional<DeliveryMutationResult> result,
        Optional<DeliveryFailureKind> failure,
        Optional<String> requestId) {
    public enum Phase {
        IDLE,
        RUNNING,
        SUCCESS,
        ERROR,
        CLOSED
    }

    public DeliveryActionState {
        Objects.requireNonNull(phase, "Delivery action phase is required.");
        action = Objects.requireNonNull(action, "Delivery action is required.");
        result = Objects.requireNonNull(result, "Delivery result is required.");
        failure = Objects.requireNonNull(failure, "Delivery failure is required.");
        requestId = Objects.requireNonNull(requestId, "Delivery request ID is required.");
    }

    public static DeliveryActionState idle() {
        return value(Phase.IDLE, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static DeliveryActionState running(DeliveryAction action) {
        return value(Phase.RUNNING, Optional.of(action), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static DeliveryActionState success(DeliveryMutationResult result) {
        return value(Phase.SUCCESS, Optional.of(result.action()), Optional.of(result),
                Optional.empty(), Optional.empty());
    }

    public static DeliveryActionState error(
            DeliveryAction action,
            DeliveryException failure) {
        return value(Phase.ERROR, Optional.of(action), Optional.empty(),
                Optional.of(failure.kind()), failure.requestId());
    }

    public static DeliveryActionState closed() {
        return value(Phase.CLOSED, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    private static DeliveryActionState value(
            Phase phase,
            Optional<DeliveryAction> action,
            Optional<DeliveryMutationResult> result,
            Optional<DeliveryFailureKind> failure,
            Optional<String> requestId) {
        return new DeliveryActionState(phase, action, result, failure, requestId);
    }
}
