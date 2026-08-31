package br.com.tresvtintas.mobile.core.commission;

import java.util.Objects;
import java.util.Optional;

public record CommissionActionState(
        Phase phase,
        Optional<CommissionAction> action,
        Optional<CommissionMutationResult> result,
        Optional<CommissionFailureKind> failure,
        Optional<String> requestId) {
    public enum Phase {
        IDLE,
        RUNNING,
        SUCCESS,
        ERROR,
        CLOSED
    }

    public CommissionActionState {
        Objects.requireNonNull(phase, "Commission action phase is required.");
        action = Objects.requireNonNull(action, "Commission action is required.");
        result = Objects.requireNonNull(result, "Commission result is required.");
        failure = Objects.requireNonNull(failure, "Commission failure is required.");
        requestId = Objects.requireNonNull(requestId, "Commission request ID is required.");
    }

    public static CommissionActionState idle() {
        return value(
                Phase.IDLE,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    public static CommissionActionState running(CommissionAction action) {
        return value(
                Phase.RUNNING,
                Optional.of(action),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    public static CommissionActionState success(CommissionMutationResult result) {
        return value(
                Phase.SUCCESS,
                Optional.of(result.action()),
                Optional.of(result),
                Optional.empty(),
                Optional.empty());
    }

    public static CommissionActionState error(
            CommissionAction action,
            CommissionException failure) {
        return value(
                Phase.ERROR,
                Optional.of(action),
                Optional.empty(),
                Optional.of(failure.kind()),
                failure.requestId());
    }

    public static CommissionActionState closed() {
        return value(
                Phase.CLOSED,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    private static CommissionActionState value(
            Phase phase,
            Optional<CommissionAction> action,
            Optional<CommissionMutationResult> result,
            Optional<CommissionFailureKind> failure,
            Optional<String> requestId) {
        return new CommissionActionState(
                phase,
                action,
                result,
                failure,
                requestId);
    }
}
