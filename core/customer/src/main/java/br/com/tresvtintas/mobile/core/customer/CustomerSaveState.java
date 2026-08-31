package br.com.tresvtintas.mobile.core.customer;

import java.util.Optional;

public record CustomerSaveState(
        Phase phase,
        Optional<CustomerMutationResult> result,
        Optional<CustomerFailureKind> failure,
        Optional<String> requestId) {
    public enum Phase {
        IDLE,
        SAVING,
        SUCCESS,
        ERROR,
        CLOSED
    }

    public CustomerSaveState {
        if (phase == null) {
            throw new IllegalArgumentException("Customer save phase is required.");
        }
        result = result == null ? Optional.empty() : result;
        failure = failure == null ? Optional.empty() : failure;
        requestId = requestId == null ? Optional.empty() : requestId;
        if (phase == Phase.SUCCESS && result.isEmpty()) {
            throw new IllegalArgumentException(
                    "Successful customer save requires a result.");
        }
    }

    public static CustomerSaveState idle() {
        return phase(Phase.IDLE);
    }

    public static CustomerSaveState saving() {
        return phase(Phase.SAVING);
    }

    public static CustomerSaveState success(CustomerMutationResult result) {
        return new CustomerSaveState(
                Phase.SUCCESS,
                Optional.of(result),
                Optional.empty(),
                Optional.empty());
    }

    public static CustomerSaveState error(CustomerException exception) {
        return new CustomerSaveState(
                Phase.ERROR,
                Optional.empty(),
                Optional.of(exception.kind()),
                exception.requestId());
    }

    public static CustomerSaveState closed() {
        return phase(Phase.CLOSED);
    }

    private static CustomerSaveState phase(Phase phase) {
        return new CustomerSaveState(
                phase,
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }
}
