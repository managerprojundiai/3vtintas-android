package br.com.tresvtintas.mobile.core.appointment;

import java.util.Optional;

public record AppointmentResponsibleState(
        Phase phase,
        Optional<AppointmentResponsibleSnapshot> snapshot,
        Optional<AppointmentFailureKind> failure,
        Optional<String> requestId) {
    public enum Phase {
        EMPTY,
        LOADING,
        READY,
        LOADING_MORE,
        ERROR,
        CLOSED
    }

    public AppointmentResponsibleState {
        snapshot = snapshot == null ? Optional.empty() : snapshot;
        failure = failure == null ? Optional.empty() : failure;
        requestId = requestId == null ? Optional.empty() : requestId;
        if (phase == null
                || ((phase == Phase.READY || phase == Phase.LOADING_MORE)
                        && snapshot.isEmpty())
                || (phase == Phase.ERROR && failure.isEmpty())) {
            throw new IllegalArgumentException(
                    "Appointment responsible state is invalid.");
        }
    }

    public static AppointmentResponsibleState empty() {
        return phase(Phase.EMPTY);
    }

    public static AppointmentResponsibleState loading() {
        return phase(Phase.LOADING);
    }

    public static AppointmentResponsibleState loadingMore(
            AppointmentResponsibleSnapshot value) {
        return new AppointmentResponsibleState(
                Phase.LOADING_MORE,
                Optional.of(value),
                Optional.empty(),
                Optional.empty());
    }

    public static AppointmentResponsibleState ready(
            AppointmentResponsibleSnapshot value) {
        return new AppointmentResponsibleState(
                Phase.READY,
                Optional.of(value),
                Optional.empty(),
                Optional.empty());
    }

    public static AppointmentResponsibleState error(
            AppointmentException failure) {
        return new AppointmentResponsibleState(
                Phase.ERROR,
                Optional.empty(),
                Optional.of(failure.kind()),
                failure.requestId());
    }

    public static AppointmentResponsibleState closed() {
        return phase(Phase.CLOSED);
    }

    private static AppointmentResponsibleState phase(Phase phase) {
        return new AppointmentResponsibleState(
                phase,
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }
}
