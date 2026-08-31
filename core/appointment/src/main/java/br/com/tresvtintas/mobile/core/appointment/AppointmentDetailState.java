package br.com.tresvtintas.mobile.core.appointment;

import java.util.Optional;

public record AppointmentDetailState(
        Phase phase,
        Optional<AppointmentDetail> detail,
        Optional<AppointmentFailureKind> failure,
        Optional<String> requestId) {
    public enum Phase {
        EMPTY,
        LOADING,
        READY,
        ERROR,
        CLOSED
    }

    public AppointmentDetailState {
        detail = detail == null ? Optional.empty() : detail;
        failure = failure == null ? Optional.empty() : failure;
        requestId = requestId == null ? Optional.empty() : requestId;
        if (phase == null
                || (phase == Phase.READY && detail.isEmpty())
                || (phase == Phase.ERROR && failure.isEmpty())) {
            throw new IllegalArgumentException(
                    "Appointment detail state is invalid.");
        }
    }

    public static AppointmentDetailState empty() {
        return phase(Phase.EMPTY);
    }

    public static AppointmentDetailState loading() {
        return phase(Phase.LOADING);
    }

    public static AppointmentDetailState ready(AppointmentDetail value) {
        return new AppointmentDetailState(
                Phase.READY,
                Optional.of(value),
                Optional.empty(),
                Optional.empty());
    }

    public static AppointmentDetailState error(AppointmentException value) {
        return new AppointmentDetailState(
                Phase.ERROR,
                Optional.empty(),
                Optional.of(value.kind()),
                value.requestId());
    }

    public static AppointmentDetailState closed() {
        return phase(Phase.CLOSED);
    }

    private static AppointmentDetailState phase(Phase phase) {
        return new AppointmentDetailState(
                phase,
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }
}
