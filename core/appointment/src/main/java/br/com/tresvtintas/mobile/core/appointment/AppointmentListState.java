package br.com.tresvtintas.mobile.core.appointment;

import java.util.Optional;

public record AppointmentListState(
        Phase phase,
        Optional<AppointmentSnapshot> snapshot,
        Optional<AppointmentFailureKind> failure,
        Optional<String> requestId) {
    public enum Phase {
        EMPTY,
        LOADING,
        READY,
        REFRESHING,
        LOADING_MORE,
        ERROR,
        CLOSED
    }

    public AppointmentListState {
        snapshot = snapshot == null ? Optional.empty() : snapshot;
        failure = failure == null ? Optional.empty() : failure;
        requestId = requestId == null ? Optional.empty() : requestId;
        if (phase == null
                || ((phase == Phase.READY
                                || phase == Phase.REFRESHING
                                || phase == Phase.LOADING_MORE)
                        && snapshot.isEmpty())) {
            throw new IllegalArgumentException(
                    "Appointment list state is invalid.");
        }
    }

    public static AppointmentListState empty() {
        return phase(Phase.EMPTY);
    }

    public static AppointmentListState loading() {
        return phase(Phase.LOADING);
    }

    public static AppointmentListState refreshing(AppointmentSnapshot value) {
        return snapshot(Phase.REFRESHING, value);
    }

    public static AppointmentListState loadingMore(AppointmentSnapshot value) {
        return snapshot(Phase.LOADING_MORE, value);
    }

    public static AppointmentListState ready(
            AppointmentSnapshot value,
            Optional<AppointmentFailureKind> warning,
            Optional<String> requestId) {
        return new AppointmentListState(
                Phase.READY,
                Optional.of(value),
                warning,
                requestId);
    }

    public static AppointmentListState error(AppointmentException failure) {
        return new AppointmentListState(
                Phase.ERROR,
                Optional.empty(),
                Optional.of(failure.kind()),
                failure.requestId());
    }

    public static AppointmentListState closed() {
        return phase(Phase.CLOSED);
    }

    private static AppointmentListState phase(Phase phase) {
        return new AppointmentListState(
                phase,
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    private static AppointmentListState snapshot(
            Phase phase,
            AppointmentSnapshot value) {
        return new AppointmentListState(
                phase,
                Optional.of(value),
                Optional.empty(),
                Optional.empty());
    }
}
