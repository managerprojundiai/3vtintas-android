package br.com.tresvtintas.mobile.core.network.dto;

import java.util.Set;

public record AgentConversationDto(
        String id,
        String title,
        String status,
        boolean createdOnThisDevice,
        String lastActivityAt,
        String createdAt,
        String updatedAt) {
    private static final int MAXIMUM_TITLE_CHARS = 120;
    private static final Set<String> STATUSES =
            Set.of("active", "archived");

    public AgentConversationDto {
        id = DtoValidation.requireUuid(
                id,
                "Agent conversation ID");
        title = DtoValidation.requireText(
                title,
                "Agent conversation title",
                MAXIMUM_TITLE_CHARS);
        if (!STATUSES.contains(status)) {
            throw new IllegalArgumentException(
                    "Agent conversation status is invalid.");
        }
        lastActivityAt = DtoValidation.requireInstant(
                lastActivityAt,
                "Agent conversation activity");
        createdAt = DtoValidation.requireInstant(
                createdAt,
                "Agent conversation creation");
        updatedAt = DtoValidation.requireInstant(
                updatedAt,
                "Agent conversation update");
    }
}
