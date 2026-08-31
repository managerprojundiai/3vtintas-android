package br.com.tresvtintas.mobile.core.organizationadmin;

import java.util.Objects;
import java.util.Optional;

public record OrganizationAdministrationTaskState<T>(
        Phase phase,
        Optional<T> value,
        Optional<OrganizationAdministrationException> failure) {
    public enum Phase {
        IDLE,
        RUNNING,
        SUCCESS,
        ERROR,
        CLOSED
    }

    public OrganizationAdministrationTaskState {
        Objects.requireNonNull(phase, "Task phase is required.");
        value = value == null ? Optional.empty() : value;
        failure = failure == null ? Optional.empty() : failure;
    }

    public static <T> OrganizationAdministrationTaskState<T> idle() {
        return state(Phase.IDLE, null, null);
    }

    public static <T> OrganizationAdministrationTaskState<T> running() {
        return state(Phase.RUNNING, null, null);
    }

    public static <T> OrganizationAdministrationTaskState<T> success(T value) {
        return state(Phase.SUCCESS, Objects.requireNonNull(value), null);
    }

    public static <T> OrganizationAdministrationTaskState<T> error(
            OrganizationAdministrationException failure) {
        return state(Phase.ERROR, null, Objects.requireNonNull(failure));
    }

    public static <T> OrganizationAdministrationTaskState<T> closed() {
        return state(Phase.CLOSED, null, null);
    }

    private static <T> OrganizationAdministrationTaskState<T> state(
            Phase phase,
            T value,
            OrganizationAdministrationException failure) {
        return new OrganizationAdministrationTaskState<>(
                phase,
                Optional.ofNullable(value),
                Optional.ofNullable(failure));
    }
}
