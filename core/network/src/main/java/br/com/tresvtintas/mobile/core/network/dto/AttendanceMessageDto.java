package br.com.tresvtintas.mobile.core.network.dto;

import java.util.Set;

public record AttendanceMessageDto(
        String id,
        String sourceId,
        String direction,
        String type,
        String content,
        boolean truncated,
        String status,
        String createdAt) {
    public AttendanceMessageDto {
        id = DtoValidation.requireText(
                id,
                "Attendance message ID",
                400);
        sourceId = DtoValidation.requireText(
                sourceId,
                "Attendance message source ID",
                160);
        if (!Set.of("inbound", "outbound").contains(direction)) {
            throw new IllegalArgumentException(
                    "Attendance message direction is invalid.");
        }
        type = DtoValidation.requireText(
                type,
                "Attendance message type",
                80);
        if (content == null || content.length() > 8_000) {
            throw new IllegalArgumentException(
                    "Attendance message content is invalid.");
        }
        status = DtoValidation.optionalText(
                status,
                "Attendance message status",
                80);
        if (status != null && status.isBlank()) {
            throw new IllegalArgumentException(
                    "Attendance message status is invalid.");
        }
        createdAt = DtoValidation.requireInstant(
                createdAt,
                "Attendance message creation");
    }
}
