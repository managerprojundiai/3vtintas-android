package br.com.tresvtintas.mobile.core.audit;

import br.com.tresvtintas.mobile.core.audit.AgentReplayModels.Turn;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record AgentReplayState(
        Phase phase,
        Optional<Snapshot> snapshot,
        Optional<AuditException> failure) {
    public enum Phase {
        EMPTY,
        LOADING,
        READY,
        REFRESHING,
        LOADING_MORE,
        STALE,
        ERROR,
        CLOSED
    }

    public AgentReplayState {
        Objects.requireNonNull(phase, "Agent replay state is required.");
        snapshot = snapshot == null ? Optional.empty() : snapshot;
        failure = failure == null ? Optional.empty() : failure;
    }

    public static AgentReplayState empty() {
        return state(Phase.EMPTY, Optional.empty(), Optional.empty());
    }

    public static AgentReplayState loading() {
        return state(Phase.LOADING, Optional.empty(), Optional.empty());
    }

    public static AgentReplayState ready(Snapshot snapshot) {
        return state(Phase.READY, Optional.of(snapshot), Optional.empty());
    }

    public static AgentReplayState busy(Snapshot snapshot, boolean more) {
        return state(
                more ? Phase.LOADING_MORE : Phase.REFRESHING,
                Optional.of(snapshot),
                Optional.empty());
    }

    public static AgentReplayState stale(Snapshot snapshot, AuditException failure) {
        return state(Phase.STALE, Optional.of(snapshot), Optional.of(failure));
    }

    public static AgentReplayState error(AuditException failure) {
        return state(Phase.ERROR, Optional.empty(), Optional.of(failure));
    }

    public static AgentReplayState closed() {
        return state(Phase.CLOSED, Optional.empty(), Optional.empty());
    }

    private static AgentReplayState state(
            Phase phase,
            Optional<Snapshot> snapshot,
            Optional<AuditException> failure) {
        return new AgentReplayState(phase, snapshot, failure);
    }

    public record Snapshot(
            AgentReplayQuery query,
            List<Turn> turns,
            Optional<String> nextCursor) {
        public Snapshot {
            Objects.requireNonNull(query, "Agent replay query is required.");
            turns = List.copyOf(turns);
            nextCursor = nextCursor == null ? Optional.empty() : nextCursor;
        }

        @Override
        public List<Turn> turns() {
            return List.copyOf(turns);
        }
    }
}
