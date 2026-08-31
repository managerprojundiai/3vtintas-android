package br.com.tresvtintas.mobile.core.audit;

import br.com.tresvtintas.mobile.core.audit.AuditModels.Event;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record AuditState(
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

    public AuditState {
        Objects.requireNonNull(phase, "Audit phase is required.");
        snapshot = snapshot == null ? Optional.empty() : snapshot;
        failure = failure == null ? Optional.empty() : failure;
    }

    public static AuditState empty() {
        return state(Phase.EMPTY, Optional.empty(), Optional.empty());
    }

    public static AuditState loading() {
        return state(Phase.LOADING, Optional.empty(), Optional.empty());
    }

    public static AuditState ready(Snapshot snapshot) {
        return state(Phase.READY, Optional.of(snapshot), Optional.empty());
    }

    public static AuditState busy(Snapshot snapshot, boolean more) {
        return state(
                more ? Phase.LOADING_MORE : Phase.REFRESHING,
                Optional.of(snapshot),
                Optional.empty());
    }

    public static AuditState stale(Snapshot snapshot, AuditException failure) {
        return state(Phase.STALE, Optional.of(snapshot), Optional.of(failure));
    }

    public static AuditState error(AuditException failure) {
        return state(Phase.ERROR, Optional.empty(), Optional.of(failure));
    }

    public static AuditState closed() {
        return state(Phase.CLOSED, Optional.empty(), Optional.empty());
    }

    private static AuditState state(
            Phase phase,
            Optional<Snapshot> snapshot,
            Optional<AuditException> failure) {
        return new AuditState(phase, snapshot, failure);
    }

    public record Snapshot(
            AuditQuery query,
            List<Event> events,
            Optional<String> nextCursor) {
        public Snapshot {
            Objects.requireNonNull(query, "Audit query is required.");
            events = List.copyOf(events);
            nextCursor = nextCursor == null ? Optional.empty() : nextCursor;
        }

        @Override
        public List<Event> events() {
            return List.copyOf(events);
        }
    }
}
