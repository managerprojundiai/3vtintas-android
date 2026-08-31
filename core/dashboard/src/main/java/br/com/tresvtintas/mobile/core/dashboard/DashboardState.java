package br.com.tresvtintas.mobile.core.dashboard;

import java.util.Optional;

public record DashboardState(
        Phase phase,
        Optional<DashboardSnapshot> snapshot,
        Optional<DashboardFailureKind> failure,
        Optional<String> requestId) {

    public enum Phase {
        EMPTY,
        LOADING,
        REFRESHING,
        READY,
        ERROR,
        CLOSED
    }

    public DashboardState {
        if (phase == null) {
            throw new IllegalArgumentException(
                    "Dashboard state phase is required.");
        }
        snapshot = snapshot == null ? Optional.empty() : snapshot;
        failure = failure == null ? Optional.empty() : failure;
        requestId = requestId == null ? Optional.empty() : requestId;
        if ((phase == Phase.READY || phase == Phase.REFRESHING)
                && snapshot.isEmpty()) {
            throw new IllegalArgumentException(
                    "Dashboard state requires a snapshot.");
        }
    }

    public static DashboardState empty() {
        return phase(Phase.EMPTY);
    }

    public static DashboardState loading() {
        return phase(Phase.LOADING);
    }

    public static DashboardState refreshing(DashboardSnapshot snapshot) {
        return ready(Phase.REFRESHING, snapshot, null);
    }

    public static DashboardState ready(DashboardSnapshot snapshot) {
        return ready(Phase.READY, snapshot, null);
    }

    public static DashboardState stale(
            DashboardSnapshot snapshot,
            DashboardException failure) {
        return ready(Phase.READY, snapshot, failure);
    }

    public static DashboardState error(DashboardException failure) {
        return new DashboardState(
                Phase.ERROR,
                Optional.empty(),
                Optional.of(failure.kind()),
                failure.requestId());
    }

    public static DashboardState closed() {
        return phase(Phase.CLOSED);
    }

    private static DashboardState phase(Phase value) {
        return new DashboardState(
                value,
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    private static DashboardState ready(
            Phase phase,
            DashboardSnapshot snapshot,
            DashboardException failure) {
        return new DashboardState(
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
