package br.com.tresvtintas.mobile.core.attendance;

import java.util.Objects;
import java.util.Optional;

public record AttendanceRealtimeState(
        Phase phase,
        Optional<AttendanceFailureKind> failure) {
    public enum Phase {
        IDLE,
        CONNECTING,
        CONNECTED,
        FALLBACK,
        CLOSED
    }

    public AttendanceRealtimeState {
        Objects.requireNonNull(phase, "Realtime phase is required.");
        failure = Objects.requireNonNull(
                failure,
                "Realtime failure is required.");
    }

    public static AttendanceRealtimeState idle() {
        return new AttendanceRealtimeState(
                Phase.IDLE,
                Optional.empty());
    }

    public static AttendanceRealtimeState connecting() {
        return new AttendanceRealtimeState(
                Phase.CONNECTING,
                Optional.empty());
    }

    public static AttendanceRealtimeState connected() {
        return new AttendanceRealtimeState(
                Phase.CONNECTED,
                Optional.empty());
    }

    public static AttendanceRealtimeState fallback(
            AttendanceFailureKind failure) {
        return new AttendanceRealtimeState(
                Phase.FALLBACK,
                Optional.of(Objects.requireNonNull(
                        failure,
                        "Realtime failure kind is required.")));
    }

    public static AttendanceRealtimeState closed() {
        return new AttendanceRealtimeState(
                Phase.CLOSED,
                Optional.empty());
    }
}
