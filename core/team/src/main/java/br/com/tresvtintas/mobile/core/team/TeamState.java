package br.com.tresvtintas.mobile.core.team;

import java.util.Optional;

public record TeamState(
        Phase phase,
        Optional<TeamSnapshot> snapshot,
        Optional<TeamFailureKind> failure,
        Optional<String> requestId) {
    public enum Phase {
        EMPTY,
        LOADING,
        REFRESHING,
        READY,
        ERROR,
        CLOSED
    }

    public TeamState {
        if (phase == null) {
            throw new IllegalArgumentException("Team state phase is required.");
        }
        snapshot = snapshot == null ? Optional.empty() : snapshot;
        failure = failure == null ? Optional.empty() : failure;
        requestId = requestId == null ? Optional.empty() : requestId;
        if ((phase == Phase.READY || phase == Phase.REFRESHING)
                && snapshot.isEmpty()) {
            throw new IllegalArgumentException(
                    "Team state requires a snapshot.");
        }
    }

    public static TeamState empty() {
        return phase(Phase.EMPTY);
    }

    public static TeamState loading() {
        return phase(Phase.LOADING);
    }

    public static TeamState refreshing(TeamSnapshot snapshot) {
        return ready(Phase.REFRESHING, snapshot, null);
    }

    public static TeamState ready(TeamSnapshot snapshot) {
        return ready(Phase.READY, snapshot, null);
    }

    public static TeamState stale(
            TeamSnapshot snapshot,
            TeamException failure) {
        return ready(Phase.READY, snapshot, failure);
    }

    public static TeamState error(TeamException failure) {
        return new TeamState(
                Phase.ERROR,
                Optional.empty(),
                Optional.of(failure.kind()),
                failure.requestId());
    }

    public static TeamState closed() {
        return phase(Phase.CLOSED);
    }

    private static TeamState phase(Phase phase) {
        return new TeamState(
                phase,
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    private static TeamState ready(
            Phase phase,
            TeamSnapshot snapshot,
            TeamException failure) {
        return new TeamState(
                phase,
                Optional.of(snapshot),
                failure == null
                        ? Optional.empty()
                        : Optional.of(failure.kind()),
                failure == null
                        ? Optional.empty()
                        : failure.requestId());
    }
}
