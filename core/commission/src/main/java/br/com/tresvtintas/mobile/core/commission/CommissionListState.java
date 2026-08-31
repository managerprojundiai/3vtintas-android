package br.com.tresvtintas.mobile.core.commission;

import java.util.Optional;

public record CommissionListState(
        Phase phase,
        Optional<CommissionSnapshot> snapshot,
        Optional<CommissionFailureKind> failure,
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

    public CommissionListState {
        if (phase == null) {
            throw new IllegalArgumentException("Commission list phase is required.");
        }
        snapshot = snapshot == null ? Optional.empty() : snapshot;
        failure = failure == null ? Optional.empty() : failure;
        requestId = requestId == null ? Optional.empty() : requestId;
        if ((phase == Phase.READY
                        || phase == Phase.REFRESHING
                        || phase == Phase.LOADING_MORE)
                && snapshot.isEmpty()) {
            throw new IllegalArgumentException(
                    "Commission list phase requires a snapshot.");
        }
    }

    public static CommissionListState empty() {
        return phase(Phase.EMPTY);
    }

    public static CommissionListState loading() {
        return phase(Phase.LOADING);
    }

    public static CommissionListState refreshing(CommissionSnapshot value) {
        return snapshot(Phase.REFRESHING, value);
    }

    public static CommissionListState loadingMore(CommissionSnapshot value) {
        return snapshot(Phase.LOADING_MORE, value);
    }

    public static CommissionListState ready(
            CommissionSnapshot value,
            Optional<CommissionFailureKind> warning,
            Optional<String> requestId) {
        return new CommissionListState(
                Phase.READY,
                Optional.of(value),
                warning,
                requestId);
    }

    public static CommissionListState error(CommissionException value) {
        return new CommissionListState(
                Phase.ERROR,
                Optional.empty(),
                Optional.of(value.kind()),
                value.requestId());
    }

    public static CommissionListState closed() {
        return phase(Phase.CLOSED);
    }

    private static CommissionListState phase(Phase value) {
        return new CommissionListState(
                value,
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    private static CommissionListState snapshot(
            Phase phase,
            CommissionSnapshot value) {
        return new CommissionListState(
                phase,
                Optional.of(value),
                Optional.empty(),
                Optional.empty());
    }
}
