package br.com.tresvtintas.mobile.core.network.dto;

import java.util.Set;

public record AgentTurnDto(
        String id,
        String conversationId,
        String status,
        String createdAt,
        String startedAt,
        String completedAt) {
    private static final Set<String> STATUSES = Set.of(
            "queued",
            "running",
            "completed",
            "failed",
            "cancelled");

    public AgentTurnDto {
        id = DtoValidation.requireUuid(id, "Agent turn ID");
        conversationId = DtoValidation.requireUuid(
                conversationId,
                "Agent turn conversation ID");
        if (!STATUSES.contains(status)) {
            throw new IllegalArgumentException(
                    "Agent turn status is invalid.");
        }
        createdAt = DtoValidation.requireInstant(
                createdAt,
                "Agent turn creation");
        startedAt = optionalInstant(startedAt, "Agent turn start");
        completedAt = optionalInstant(
                completedAt,
                "Agent turn completion");
    }

    private static String optionalInstant(
            String value,
            String fieldName) {
        return value == null
                ? null
                : DtoValidation.requireInstant(value, fieldName);
    }
}
