package br.com.tresvtintas.mobile.core.network.dto;

public record AttendanceReadCursorResponse(
        String conversationId,
        String readThroughMessageId,
        int unreadCount,
        String updatedAt) {
    public AttendanceReadCursorResponse {
        conversationId = DtoValidation.requireText(
                conversationId,
                "Attendance conversation ID",
                200);
        readThroughMessageId = DtoValidation.requireText(
                readThroughMessageId,
                "Attendance read message ID",
                400);
        updatedAt = DtoValidation.requireInstant(
                updatedAt,
                "Attendance read cursor update");
        if (unreadCount < 0) {
            throw new IllegalArgumentException(
                    "Attendance unread count is invalid.");
        }
    }
}
