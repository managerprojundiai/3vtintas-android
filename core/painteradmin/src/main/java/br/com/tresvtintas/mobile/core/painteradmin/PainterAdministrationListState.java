package br.com.tresvtintas.mobile.core.painteradmin;

import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.AccessRequest;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.Options;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.Painter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record PainterAdministrationListState(
        Phase phase,
        Optional<Snapshot> snapshot,
        Optional<PainterAdministrationException> failure) {
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

    public PainterAdministrationListState {
        Objects.requireNonNull(phase, "Painter state phase is required.");
        snapshot = snapshot == null ? Optional.empty() : snapshot;
        failure = failure == null ? Optional.empty() : failure;
    }

    public static PainterAdministrationListState empty() {
        return new PainterAdministrationListState(
                Phase.EMPTY,
                Optional.empty(),
                Optional.empty());
    }

    public static PainterAdministrationListState loading() {
        return new PainterAdministrationListState(
                Phase.LOADING,
                Optional.empty(),
                Optional.empty());
    }

    public static PainterAdministrationListState ready(Snapshot snapshot) {
        return new PainterAdministrationListState(
                Phase.READY,
                Optional.of(snapshot),
                Optional.empty());
    }

    public static PainterAdministrationListState busy(
            Snapshot snapshot,
            boolean more) {
        return new PainterAdministrationListState(
                more ? Phase.LOADING_MORE : Phase.REFRESHING,
                Optional.of(snapshot),
                Optional.empty());
    }

    public static PainterAdministrationListState stale(
            Snapshot snapshot,
            PainterAdministrationException failure) {
        return new PainterAdministrationListState(
                Phase.STALE,
                Optional.of(snapshot),
                Optional.of(failure));
    }

    public static PainterAdministrationListState error(
            PainterAdministrationException failure) {
        return new PainterAdministrationListState(
                Phase.ERROR,
                Optional.empty(),
                Optional.of(failure));
    }

    public static PainterAdministrationListState closed() {
        return new PainterAdministrationListState(
                Phase.CLOSED,
                Optional.empty(),
                Optional.empty());
    }

    public record Snapshot(
            Options options,
            PainterAdministrationQuery query,
            List<Painter> painters,
            Optional<String> painterCursor,
            List<AccessRequest> requests,
            Optional<String> requestCursor) {
        public Snapshot {
            Objects.requireNonNull(options, "Options are required.");
            Objects.requireNonNull(query, "Painter query is required.");
            painters = List.copyOf(painters);
            painterCursor = painterCursor == null
                    ? Optional.empty()
                    : painterCursor;
            requests = List.copyOf(requests);
            requestCursor = requestCursor == null
                    ? Optional.empty()
                    : requestCursor;
        }
    }
}
