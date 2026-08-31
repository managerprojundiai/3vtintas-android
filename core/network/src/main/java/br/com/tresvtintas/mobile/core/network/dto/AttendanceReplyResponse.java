package br.com.tresvtintas.mobile.core.network.dto;

import java.util.Set;

public record AttendanceReplyResponse(
        String conversationId,
        AttendanceMessageDto message,
        String deliveryState) {
    public AttendanceReplyResponse {
        conversationId = DtoValidation.requireText(
                conversationId,
                "Attendance conversation ID",
                200);
        if (message == null
                || !Set.of("available", "queued")
                        .contains(deliveryState)) {
            throw new IllegalArgumentException(
                    "Attendance reply response is invalid.");
        }
    }
}
