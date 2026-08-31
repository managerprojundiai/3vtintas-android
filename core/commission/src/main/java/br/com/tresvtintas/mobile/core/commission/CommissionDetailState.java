package br.com.tresvtintas.mobile.core.commission;

import java.util.Optional;

public record CommissionDetailState(
        Phase phase,
        Optional<CommissionDetail> detail,
        Optional<CommissionFailureKind> failure,
        Optional<String> requestId) {
    public enum Phase {
        EMPTY,
        LOADING,
        READY,
        ERROR,
        CLOSED
    }

    public CommissionDetailState {
        if (phase == null) {
            throw new IllegalArgumentException("Commission detail phase is required.");
        }
        detail = detail == null ? Optional.empty() : detail;
        failure = failure == null ? Optional.empty() : failure;
        requestId = requestId == null ? Optional.empty() : requestId;
        if (phase == Phase.READY && detail.isEmpty()) {
            throw new IllegalArgumentException(
                    "Ready commission detail requires a value.");
        }
    }

    public static CommissionDetailState empty() {
        return phase(Phase.EMPTY);
    }

    public static CommissionDetailState loading() {
        return phase(Phase.LOADING);
    }

    public static CommissionDetailState ready(CommissionDetail value) {
        return new CommissionDetailState(
                Phase.READY,
                Optional.of(value),
                Optional.empty(),
                Optional.empty());
    }

    public static CommissionDetailState error(CommissionException value) {
        return new CommissionDetailState(
                Phase.ERROR,
                Optional.empty(),
                Optional.of(value.kind()),
                value.requestId());
    }

    public static CommissionDetailState closed() {
        return phase(Phase.CLOSED);
    }

    private static CommissionDetailState phase(Phase value) {
        return new CommissionDetailState(
                value,
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }
}
