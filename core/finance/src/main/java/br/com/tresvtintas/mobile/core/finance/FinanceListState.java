package br.com.tresvtintas.mobile.core.finance;

import java.util.Optional;

public record FinanceListState(
        Phase phase,
        Optional<FinanceSnapshot> snapshot,
        Optional<FinanceFailureKind> failure,
        Optional<String> requestId) {
    public enum Phase {
        EMPTY,
        LOADING,
        READY,
        REFRESHING,
        LOADING_MORE,
        ERROR,
        CLOSED
    }

    public FinanceListState {
        if (phase == null) {
            throw new IllegalArgumentException("Finance list phase is required.");
        }
        snapshot = snapshot == null ? Optional.empty() : snapshot;
        failure = failure == null ? Optional.empty() : failure;
        requestId = requestId == null ? Optional.empty() : requestId;
        if ((phase == Phase.READY
                        || phase == Phase.REFRESHING
                        || phase == Phase.LOADING_MORE)
                && snapshot.isEmpty()) {
            throw new IllegalArgumentException(
                    "Finance list phase requires a snapshot.");
        }
    }

    public static FinanceListState empty() {
        return phase(Phase.EMPTY);
    }

    public static FinanceListState loading() {
        return phase(Phase.LOADING);
    }

    public static FinanceListState refreshing(FinanceSnapshot value) {
        return snapshot(Phase.REFRESHING, value);
    }

    public static FinanceListState loadingMore(FinanceSnapshot value) {
        return snapshot(Phase.LOADING_MORE, value);
    }

    public static FinanceListState ready(
            FinanceSnapshot value,
            Optional<FinanceFailureKind> warning,
            Optional<String> requestId) {
        return new FinanceListState(
                Phase.READY,
                Optional.of(value),
                warning,
                requestId);
    }

    public static FinanceListState error(FinanceException value) {
        return new FinanceListState(
                Phase.ERROR,
                Optional.empty(),
                Optional.of(value.kind()),
                value.requestId());
    }

    public static FinanceListState closed() {
        return phase(Phase.CLOSED);
    }

    private static FinanceListState phase(Phase value) {
        return new FinanceListState(
                value,
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    private static FinanceListState snapshot(
            Phase phase,
            FinanceSnapshot value) {
        return new FinanceListState(
                phase,
                Optional.of(value),
                Optional.empty(),
                Optional.empty());
    }
}
