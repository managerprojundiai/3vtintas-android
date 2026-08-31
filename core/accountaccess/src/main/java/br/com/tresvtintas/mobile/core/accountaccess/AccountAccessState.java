package br.com.tresvtintas.mobile.core.accountaccess;

import java.util.Optional;

public record AccountAccessState(
        Phase phase,
        AccountAccessView view,
        Optional<AccountAccessSnapshot> snapshot,
        Optional<String> mutatingId,
        Optional<AccountAccessFailureKind> failure,
        Optional<String> requestId) {
    public enum Phase {
        EMPTY,
        LOADING,
        READY,
        REFRESHING,
        LOADING_MORE,
        REVOKING,
        ERROR,
        CLOSED
    }

    public AccountAccessState {
        if (phase == null || view == null) {
            throw new IllegalArgumentException(
                    "Account access phase and view are required.");
        }
        snapshot = snapshot == null ? Optional.empty() : snapshot;
        mutatingId = mutatingId == null ? Optional.empty() : mutatingId;
        failure = failure == null ? Optional.empty() : failure;
        requestId = requestId == null ? Optional.empty() : requestId;
        if (snapshot.isPresent()
                && snapshot.orElseThrow().view() != view) {
            throw new IllegalArgumentException(
                    "Account access state view changed.");
        }
        boolean snapshotRequired = phase == Phase.READY
                || phase == Phase.REFRESHING
                || phase == Phase.LOADING_MORE
                || phase == Phase.REVOKING;
        if (snapshotRequired && snapshot.isEmpty()) {
            throw new IllegalArgumentException(
                    "Account access phase requires a snapshot.");
        }
        if (phase == Phase.REVOKING && mutatingId.isEmpty()) {
            throw new IllegalArgumentException(
                    "Revocation phase requires a target.");
        }
        if (phase != Phase.REVOKING && mutatingId.isPresent()) {
            throw new IllegalArgumentException(
                    "Only revocation may retain a target.");
        }
    }

    public static AccountAccessState empty(AccountAccessView view) {
        return phase(Phase.EMPTY, view);
    }

    public static AccountAccessState loading(AccountAccessView view) {
        return phase(Phase.LOADING, view);
    }

    public static AccountAccessState refreshing(
            AccountAccessSnapshot snapshot) {
        return withSnapshot(Phase.REFRESHING, snapshot);
    }

    public static AccountAccessState loadingMore(
            AccountAccessSnapshot snapshot) {
        return withSnapshot(Phase.LOADING_MORE, snapshot);
    }

    public static AccountAccessState revoking(
            AccountAccessSnapshot snapshot,
            String targetId) {
        return new AccountAccessState(
                Phase.REVOKING,
                snapshot.view(),
                Optional.of(snapshot),
                Optional.of(targetId),
                Optional.empty(),
                Optional.empty());
    }

    public static AccountAccessState ready(
            AccountAccessSnapshot snapshot,
            Optional<AccountAccessFailureKind> warning,
            Optional<String> requestId) {
        return new AccountAccessState(
                Phase.READY,
                snapshot.view(),
                Optional.of(snapshot),
                Optional.empty(),
                warning,
                requestId);
    }

    public static AccountAccessState error(
            AccountAccessView view,
            AccountAccessException failure) {
        return new AccountAccessState(
                Phase.ERROR,
                view,
                Optional.empty(),
                Optional.empty(),
                Optional.of(failure.kind()),
                failure.requestId());
    }

    public static AccountAccessState closed(AccountAccessView view) {
        return phase(Phase.CLOSED, view);
    }

    private static AccountAccessState phase(
            Phase phase,
            AccountAccessView view) {
        return new AccountAccessState(
                phase,
                view,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    private static AccountAccessState withSnapshot(
            Phase phase,
            AccountAccessSnapshot snapshot) {
        return new AccountAccessState(
                phase,
                snapshot.view(),
                Optional.of(snapshot),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }
}
