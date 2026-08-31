package br.com.tresvtintas.mobile.core.painteradmin;

import java.util.Objects;
import java.util.Optional;

public record PainterAdministrationTaskState<T>(
        Phase phase,
        Optional<T> result,
        Optional<PainterAdministrationException> failure) {
    public enum Phase {
        IDLE,
        RUNNING,
        SUCCESS,
        ERROR,
        CLOSED
    }

    public PainterAdministrationTaskState {
        Objects.requireNonNull(phase, "Task phase is required.");
        result = result == null ? Optional.empty() : result;
        failure = failure == null ? Optional.empty() : failure;
    }

    public static <T> PainterAdministrationTaskState<T> idle() {
        return state(Phase.IDLE, Optional.empty(), Optional.empty());
    }

    public static <T> PainterAdministrationTaskState<T> running() {
        return state(Phase.RUNNING, Optional.empty(), Optional.empty());
    }

    public static <T> PainterAdministrationTaskState<T> success(T result) {
        return state(Phase.SUCCESS, Optional.of(result), Optional.empty());
    }

    public static <T> PainterAdministrationTaskState<T> error(
            PainterAdministrationException failure) {
        return state(Phase.ERROR, Optional.empty(), Optional.of(failure));
    }

    public static <T> PainterAdministrationTaskState<T> closed() {
        return state(Phase.CLOSED, Optional.empty(), Optional.empty());
    }

    private static <T> PainterAdministrationTaskState<T> state(
            Phase phase,
            Optional<T> result,
            Optional<PainterAdministrationException> failure) {
        return new PainterAdministrationTaskState<>(phase, result, failure);
    }
}
