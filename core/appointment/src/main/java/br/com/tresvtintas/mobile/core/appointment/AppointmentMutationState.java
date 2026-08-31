package br.com.tresvtintas.mobile.core.appointment;

import java.util.Optional;

public record AppointmentMutationState(
        Phase phase,
        Optional<AppointmentMutationAction> action,
        Optional<AppointmentMutationResult> result,
        Optional<AppointmentFailureKind> failure,
        Optional<String> requestId) {
    public enum Phase {
        IDLE,
        RUNNING,
        SUCCESS,
        ERROR,
        CLOSED
    }

    public AppointmentMutationState {
        action = action == null ? Optional.empty() : action;
        result = result == null ? Optional.empty() : result;
        failure = failure == null ? Optional.empty() : failure;
        requestId = requestId == null ? Optional.empty() : requestId;
        if (phase == null
                || (phase == Phase.RUNNING && action.isEmpty())
                || (phase == Phase.SUCCESS
                        && (action.isEmpty() || result.isEmpty()))
                || (phase == Phase.ERROR
                        && (action.isEmpty() || failure.isEmpty()))) {
            throw new IllegalArgumentException(
                    "Appointment mutation state is invalid.");
        }
    }

    public static AppointmentMutationState idle() {
        return phase(Phase.IDLE);
    }

    public static AppointmentMutationState running(
            AppointmentMutationAction action) {
        return new AppointmentMutationState(
                Phase.RUNNING,
                Optional.of(action),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    public static AppointmentMutationState success(
            AppointmentMutationAction action,
            AppointmentMutationResult result) {
        return new AppointmentMutationState(
                Phase.SUCCESS,
                Optional.of(action),
                Optional.of(result),
                Optional.empty(),
                Optional.empty());
    }

    public static AppointmentMutationState error(
            AppointmentMutationAction action,
            AppointmentException failure) {
        return new AppointmentMutationState(
                Phase.ERROR,
                Optional.of(action),
                Optional.empty(),
                Optional.of(failure.kind()),
                failure.requestId());
    }

    public static AppointmentMutationState closed() {
        return phase(Phase.CLOSED);
    }

    private static AppointmentMutationState phase(Phase phase) {
        return new AppointmentMutationState(
                phase,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }
}
