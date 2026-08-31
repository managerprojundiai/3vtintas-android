package br.com.tresvtintas.mobile.core.network.dto;

import java.util.Set;

public record AgentAttendanceLatestMessageDto(
        String direction,
        String preview,
        String createdAt) {
    private static final Set<String> DIRECTIONS =
            Set.of("inbound", "outbound");

    public AgentAttendanceLatestMessageDto {
        if (!DIRECTIONS.contains(direction)
                || preview == null
                || preview.length() > 240) {
            throw new IllegalArgumentException(
                    "Agent attendance latest message is invalid.");
        }
        createdAt = DtoValidation.requireInstant(
                createdAt,
                "Agent attendance latest message timestamp");
    }
}
