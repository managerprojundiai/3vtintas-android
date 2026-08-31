package br.com.tresvtintas.mobile.core.useradmin;

import java.util.Objects;
import java.util.Optional;

public record UserAdministrationTaskState<T>(
        Phase phase,
        Optional<T> result,
        Optional<UserAdministrationException> failure) {
    public enum Phase { IDLE, RUNNING, SUCCESS, ERROR, CLOSED }

    public UserAdministrationTaskState {
        Objects.requireNonNull(phase, "Task phase is required.");
        result = result == null ? Optional.empty() : result;
        failure = failure == null ? Optional.empty() : failure;
    }

    public static <T> UserAdministrationTaskState<T> idle() {
        return state(Phase.IDLE, Optional.empty(), Optional.empty());
    }

    public static <T> UserAdministrationTaskState<T> running() {
        return state(Phase.RUNNING, Optional.empty(), Optional.empty());
    }

    public static <T> UserAdministrationTaskState<T> success(T result) {
        return state(Phase.SUCCESS, Optional.of(result), Optional.empty());
    }

    public static <T> UserAdministrationTaskState<T> error(
            UserAdministrationException failure) {
        return state(Phase.ERROR, Optional.empty(), Optional.of(failure));
    }

    public static <T> UserAdministrationTaskState<T> closed() {
        return state(Phase.CLOSED, Optional.empty(), Optional.empty());
    }

    private static <T> UserAdministrationTaskState<T> state(
            Phase phase,
            Optional<T> result,
            Optional<UserAdministrationException> failure) {
        return new UserAdministrationTaskState<>(phase, result, failure);
    }
}
