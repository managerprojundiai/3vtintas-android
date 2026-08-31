package br.com.tresvtintas.mobile.core.attendance;

import java.time.Instant;
import java.util.Objects;

public record AttendanceReadCursorResult(
        String conversationId,
        String readThroughMessageId,
        int unreadCount,
        Instant updatedAt) {
    public AttendanceReadCursorResult {
        conversationId = AttendanceConversationId.require(
                conversationId);
        String prefix = conversationId + ":";
        String sourceId = readThroughMessageId != null
                && readThroughMessageId.startsWith(prefix)
                ? readThroughMessageId.substring(prefix.length())
                : "";
        if (readThroughMessageId == null
                || readThroughMessageId.isBlank()
                || readThroughMessageId.length() > 400
                || !sourceId.matches("[1-9]\\d*")
                || unreadCount < 0) {
            throw new IllegalArgumentException(
                    "Attendance read cursor result is invalid.");
        }
        Objects.requireNonNull(
                updatedAt,
                "Attendance read cursor update is required.");
    }
}
