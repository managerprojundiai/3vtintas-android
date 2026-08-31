package br.com.tresvtintas.mobile.core.finance;

import java.util.Optional;

public record FinanceMutationState(
        Phase phase,
        Optional<FinanceAction> action,
        Optional<FinanceMutationResult> result,
        Optional<FinanceFailureKind> failure,
        Optional<String> requestId) {
    public enum Phase {
        IDLE,
        RUNNING,
        SUCCESS,
        ERROR,
        CLOSED
    }

    public FinanceMutationState {
        if (phase == null) {
            throw new IllegalArgumentException(
                    "Finance mutation phase is required.");
        }
        action = action == null ? Optional.empty() : action;
        result = result == null ? Optional.empty() : result;
        failure = failure == null ? Optional.empty() : failure;
        requestId = requestId == null ? Optional.empty() : requestId;
        if (phase == Phase.RUNNING && action.isEmpty()) {
            throw new IllegalArgumentException(
                    "Running finance mutation requires an action.");
        }
        if (phase == Phase.SUCCESS
                && (action.isEmpty() || result.isEmpty())) {
            throw new IllegalArgumentException(
                    "Successful finance mutation requires a result.");
        }
        if (phase == Phase.ERROR
                && (action.isEmpty() || failure.isEmpty())) {
            throw new IllegalArgumentException(
                    "Failed finance mutation requires a failure.");
        }
    }

    public static FinanceMutationState idle() {
        return phase(Phase.IDLE);
    }

    public static FinanceMutationState running(FinanceAction action) {
        return new FinanceMutationState(
                Phase.RUNNING,
                Optional.of(action),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    public static FinanceMutationState success(FinanceMutationResult result) {
        return new FinanceMutationState(
                Phase.SUCCESS,
                Optional.of(result.action()),
                Optional.of(result),
                Optional.empty(),
                Optional.empty());
    }

    public static FinanceMutationState error(
            FinanceAction action,
            FinanceException failure) {
        return new FinanceMutationState(
                Phase.ERROR,
                Optional.of(action),
                Optional.empty(),
                Optional.of(failure.kind()),
                failure.requestId());
    }

    public static FinanceMutationState closed() {
        return phase(Phase.CLOSED);
    }

    private static FinanceMutationState phase(Phase phase) {
        return new FinanceMutationState(
                phase,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }
}
