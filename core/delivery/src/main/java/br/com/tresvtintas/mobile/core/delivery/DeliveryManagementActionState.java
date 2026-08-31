package br.com.tresvtintas.mobile.core.delivery;

import java.util.Objects;
import java.util.Optional;

public record DeliveryManagementActionState(
        Phase phase,
        Optional<DeliveryManagementAction> action,
        Optional<DeliveryManagementMutationResult> result,
        Optional<DeliveryFailureKind> failure,
        Optional<String> requestId) {
    public enum Phase {
        IDLE,
        RUNNING,
        SUCCESS,
        ERROR,
        CLOSED
    }

    public DeliveryManagementActionState {
        Objects.requireNonNull(phase, "Management action phase is required.");
        action = Objects.requireNonNull(action, "Management action is required.");
        result = Objects.requireNonNull(result, "Management result is required.");
        failure = Objects.requireNonNull(failure, "Management failure is required.");
        requestId = Objects.requireNonNull(requestId, "Management request ID is required.");
    }

    public static DeliveryManagementActionState idle() {
        return value(Phase.IDLE, null, null, null);
    }

    public static DeliveryManagementActionState running(
            DeliveryManagementAction action) {
        return value(Phase.RUNNING, action, null, null);
    }

    public static DeliveryManagementActionState success(
            DeliveryManagementMutationResult result) {
        return new DeliveryManagementActionState(
                Phase.SUCCESS,
                Optional.of(result.action()),
                Optional.of(result),
                Optional.empty(),
                Optional.empty());
    }

    public static DeliveryManagementActionState error(
            DeliveryManagementAction action,
            DeliveryException failure) {
        return new DeliveryManagementActionState(
                Phase.ERROR,
                Optional.of(action),
                Optional.empty(),
                Optional.of(failure.kind()),
                failure.requestId());
    }

    public static DeliveryManagementActionState closed() {
        return value(Phase.CLOSED, null, null, null);
    }

    private static DeliveryManagementActionState value(
            Phase phase,
            DeliveryManagementAction action,
            DeliveryManagementMutationResult result,
            DeliveryException failure) {
        return new DeliveryManagementActionState(
                phase,
                Optional.ofNullable(action),
                Optional.ofNullable(result),
                failure == null
                        ? Optional.empty()
                        : Optional.of(failure.kind()),
                failure == null
                        ? Optional.empty()
                        : failure.requestId());
    }
}
