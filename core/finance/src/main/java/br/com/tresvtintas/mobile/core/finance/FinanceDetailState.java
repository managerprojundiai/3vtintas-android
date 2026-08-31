package br.com.tresvtintas.mobile.core.finance;

import java.util.Optional;

public record FinanceDetailState(
        Phase phase,
        Optional<FinanceDetail> detail,
        Optional<FinanceFailureKind> failure,
        Optional<String> requestId) {
    public enum Phase {
        EMPTY,
        LOADING,
        READY,
        ERROR,
        CLOSED
    }

    public FinanceDetailState {
        if (phase == null) {
            throw new IllegalArgumentException("Finance detail phase is required.");
        }
        detail = detail == null ? Optional.empty() : detail;
        failure = failure == null ? Optional.empty() : failure;
        requestId = requestId == null ? Optional.empty() : requestId;
        if (phase == Phase.READY && detail.isEmpty()) {
            throw new IllegalArgumentException(
                    "Ready finance detail requires a value.");
        }
    }

    public static FinanceDetailState empty() {
        return phase(Phase.EMPTY);
    }

    public static FinanceDetailState loading() {
        return phase(Phase.LOADING);
    }

    public static FinanceDetailState ready(FinanceDetail value) {
        return new FinanceDetailState(
                Phase.READY,
                Optional.of(value),
                Optional.empty(),
                Optional.empty());
    }

    public static FinanceDetailState error(FinanceException value) {
        return new FinanceDetailState(
                Phase.ERROR,
                Optional.empty(),
                Optional.of(value.kind()),
                value.requestId());
    }

    public static FinanceDetailState closed() {
        return phase(Phase.CLOSED);
    }

    private static FinanceDetailState phase(Phase value) {
        return new FinanceDetailState(
                value,
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }
}
