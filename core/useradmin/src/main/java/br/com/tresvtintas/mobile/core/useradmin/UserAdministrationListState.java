package br.com.tresvtintas.mobile.core.useradmin;

import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationModels.Options;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationModels.User;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record UserAdministrationListState(
        Phase phase,
        Optional<Snapshot> snapshot,
        Optional<UserAdministrationException> failure) {
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

    public UserAdministrationListState {
        Objects.requireNonNull(phase, "User state phase is required.");
        snapshot = snapshot == null ? Optional.empty() : snapshot;
        failure = failure == null ? Optional.empty() : failure;
    }

    public static UserAdministrationListState empty() {
        return state(Phase.EMPTY, Optional.empty(), Optional.empty());
    }

    public static UserAdministrationListState loading() {
        return state(Phase.LOADING, Optional.empty(), Optional.empty());
    }

    public static UserAdministrationListState ready(Snapshot snapshot) {
        return state(Phase.READY, Optional.of(snapshot), Optional.empty());
    }

    public static UserAdministrationListState busy(Snapshot snapshot, boolean more) {
        return state(
                more ? Phase.LOADING_MORE : Phase.REFRESHING,
                Optional.of(snapshot),
                Optional.empty());
    }

    public static UserAdministrationListState stale(
            Snapshot snapshot,
            UserAdministrationException failure) {
        return state(Phase.STALE, Optional.of(snapshot), Optional.of(failure));
    }

    public static UserAdministrationListState error(UserAdministrationException failure) {
        return state(Phase.ERROR, Optional.empty(), Optional.of(failure));
    }

    public static UserAdministrationListState closed() {
        return state(Phase.CLOSED, Optional.empty(), Optional.empty());
    }

    private static UserAdministrationListState state(
            Phase phase,
            Optional<Snapshot> snapshot,
            Optional<UserAdministrationException> failure) {
        return new UserAdministrationListState(phase, snapshot, failure);
    }

    public record Snapshot(
            Options options,
            UserAdministrationQuery query,
            List<User> users,
            Optional<String> nextCursor) {
        public Snapshot {
            Objects.requireNonNull(options, "Options are required.");
            Objects.requireNonNull(query, "User query is required.");
            users = List.copyOf(users);
            nextCursor = nextCursor == null ? Optional.empty() : nextCursor;
        }

        @Override
        public List<User> users() {
            return List.copyOf(users);
        }
    }
}
