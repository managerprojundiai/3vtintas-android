package br.com.tresvtintas.mobile.core.appointment;

import java.util.Objects;
import java.util.Optional;

public record AgendaState(
        Phase phase,
        Optional<AgendaMonthSnapshot> snapshot,
        Optional<AgendaFailureKind> failure,
        Optional<String> requestId) {
    public enum Phase {
        EMPTY,
        LOADING,
        READY,
        REFRESHING,
        ERROR,
        CLOSED
    }

    public AgendaState {
        phase = Objects.requireNonNull(phase, "Agenda phase is required.");
        snapshot = snapshot == null ? Optional.empty() : snapshot;
        failure = failure == null ? Optional.empty() : failure;
        requestId = requestId == null ? Optional.empty() : requestId;
        if ((phase == Phase.LOADING
                        || phase == Phase.READY
                        || phase == Phase.REFRESHING)
                != snapshot.isPresent()) {
            throw new IllegalArgumentException("Agenda state is invalid.");
        }
        if (phase == Phase.ERROR && failure.isEmpty()) {
            throw new IllegalArgumentException(
                    "Agenda error state requires a failure.");
        }
    }

    public static AgendaState empty() {
        return phase(Phase.EMPTY);
    }

    public static AgendaState loading(AgendaMonthSnapshot snapshot) {
        return new AgendaState(
                Phase.LOADING,
                Optional.of(snapshot),
                Optional.empty(),
                Optional.empty());
    }

    public static AgendaState refreshing(AgendaMonthSnapshot snapshot) {
        return new AgendaState(
                Phase.REFRESHING,
                Optional.of(snapshot),
                Optional.empty(),
                Optional.empty());
    }

    public static AgendaState ready(
            AgendaMonthSnapshot snapshot,
            Optional<AgendaFailureKind> warning,
            Optional<String> requestId) {
        return new AgendaState(
                Phase.READY,
                Optional.of(snapshot),
                warning,
                requestId);
    }

    public static AgendaState error(
            AgendaFailureKind failure,
            Optional<String> requestId) {
        return new AgendaState(
                Phase.ERROR,
                Optional.empty(),
                Optional.of(failure),
                requestId);
    }

    public static AgendaState closed() {
        return phase(Phase.CLOSED);
    }

    private static AgendaState phase(Phase phase) {
        return new AgendaState(
                phase,
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }
}
