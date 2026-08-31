package br.com.tresvtintas.mobile.core.attendance;

import java.util.Optional;

@FunctionalInterface
public interface AttendanceRepository {
    AttendancePage page(
            AttendanceQuery query,
            Optional<String> cursor) throws AttendanceException;

    default AttendanceConversation conversation(
            String conversationId) throws AttendanceException {
        throw new AttendanceException(
                AttendanceFailureKind.PROTOCOL,
                "Attendance detail is not implemented.");
    }

    default AttendanceMessagePage messagePage(
            String conversationId,
            Optional<String> cursor,
            int limit) throws AttendanceException {
        throw new AttendanceException(
                AttendanceFailureKind.PROTOCOL,
                "Attendance messages are not implemented.");
    }

    default AttendanceReplyResult reply(
            String conversationId,
            String content,
            String idempotencyKey) throws AttendanceException {
        throw new AttendanceException(
                AttendanceFailureKind.PROTOCOL,
                "Attendance replies are not implemented.");
    }

    default AttendanceAssigneePage assignees(
            String conversationId,
            Optional<String> search,
            Optional<String> cursor,
            int limit) throws AttendanceException {
        throw new AttendanceException(
                AttendanceFailureKind.PROTOCOL,
                "Attendance assignees are not implemented.");
    }

    default AttendanceManagementResult manage(
            String conversationId,
            int expectedRevision,
            AttendanceManagementSelection selection,
            String idempotencyKey) throws AttendanceException {
        throw new AttendanceException(
                AttendanceFailureKind.PROTOCOL,
                "Attendance management is not implemented.");
    }

    default AttendanceReadCursorResult markRead(
            String conversationId,
            String throughMessageId) throws AttendanceException {
        throw new AttendanceException(
                AttendanceFailureKind.PROTOCOL,
                "Attendance read cursor is not implemented.");
    }
}
