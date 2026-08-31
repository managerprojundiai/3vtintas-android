package br.com.tresvtintas.mobile.core.attendance;

import java.util.Objects;

public record AttendanceReplyResult(
        String conversationId,
        AttendanceMessage message,
        AttendanceReplyDeliveryState deliveryState,
        boolean replayed) {
    public AttendanceReplyResult {
        conversationId = AttendanceConversationId.require(conversationId);
        message = Objects.requireNonNull(
                message,
                "Attendance reply message is required.");
        deliveryState = Objects.requireNonNull(
                deliveryState,
                "Attendance reply delivery state is required.");
        if (message.direction() != AttendanceMessageDirection.OUTBOUND) {
            throw new IllegalArgumentException(
                    "Attendance reply message must be outbound.");
        }
    }
}
