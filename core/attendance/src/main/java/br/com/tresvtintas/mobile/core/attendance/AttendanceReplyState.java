package br.com.tresvtintas.mobile.core.attendance;

import java.util.Optional;

public record AttendanceReplyState(
        Phase phase,
        Optional<AttendanceReplyResult> result,
        Optional<AttendanceFailureKind> failure,
        Optional<String> requestId) {
    public enum Phase {
        IDLE,
        SENDING,
        SUCCESS,
        ERROR,
        CLOSED
    }

    public AttendanceReplyState {
        result = result == null ? Optional.empty() : result;
        failure = failure == null ? Optional.empty() : failure;
        requestId = requestId == null ? Optional.empty() : requestId;
        if (phase == null
                || (phase == Phase.SUCCESS && result.isEmpty())
                || (phase == Phase.ERROR && failure.isEmpty())
                || (phase != Phase.SUCCESS && result.isPresent())
                || (phase != Phase.ERROR
                        && (failure.isPresent() || requestId.isPresent()))) {
            throw new IllegalArgumentException(
                    "Attendance reply state is invalid.");
        }
    }

    public static AttendanceReplyState idle() {
        return simple(Phase.IDLE);
    }

    public static AttendanceReplyState sending() {
        return simple(Phase.SENDING);
    }

    public static AttendanceReplyState success(
            AttendanceReplyResult result) {
        return new AttendanceReplyState(
                Phase.SUCCESS,
                Optional.of(result),
                Optional.empty(),
                Optional.empty());
    }

    public static AttendanceReplyState error(
            AttendanceException failure) {
        return new AttendanceReplyState(
                Phase.ERROR,
                Optional.empty(),
                Optional.of(failure.kind()),
                failure.requestId());
    }

    public static AttendanceReplyState closed() {
        return simple(Phase.CLOSED);
    }

    private static AttendanceReplyState simple(Phase phase) {
        return new AttendanceReplyState(
                phase,
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }
}
