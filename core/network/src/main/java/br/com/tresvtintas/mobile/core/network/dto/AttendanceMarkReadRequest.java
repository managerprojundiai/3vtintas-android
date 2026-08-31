package br.com.tresvtintas.mobile.core.network.dto;

public record AttendanceMarkReadRequest(
        String throughMessageId,
        String confirmation) {
    public AttendanceMarkReadRequest {
        throughMessageId = DtoValidation.requireText(
                throughMessageId,
                "Attendance read message ID",
                400);
        if (!"MARK_ATTENDANCE_READ".equals(confirmation)) {
            throw new IllegalArgumentException(
                    "Attendance read confirmation is invalid.");
        }
    }
}
