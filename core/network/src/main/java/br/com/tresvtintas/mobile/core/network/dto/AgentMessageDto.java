package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record AgentMessageDto(
        long id,
        String role,
        String content,
        List<AgentDocumentDto> documents,
        List<AgentActionDto> actions,
        String turnId,
        String createdAt) {
    private static final int MAXIMUM_CONTENT_CHARS = 8_000;
    private static final Set<String> ROLES =
            Set.of("user", "assistant");

    public AgentMessageDto {
        id = DtoValidation.requirePositive(id, "Agent message ID");
        if (!ROLES.contains(role)) {
            throw new IllegalArgumentException(
                    "Agent message role is invalid.");
        }
        content = DtoValidation.optionalText(
                content,
                "Agent message content",
                MAXIMUM_CONTENT_CHARS);
        if (content == null) {
            throw new IllegalArgumentException(
                    "Agent message content is required.");
        }
        documents = List.copyOf(Objects.requireNonNull(
                documents,
                "Agent message documents are required."));
        if (documents.size() > 1
                || ("user".equals(role) && !documents.isEmpty())) {
            throw new IllegalArgumentException(
                    "Agent message documents are invalid.");
        }
        actions = List.copyOf(Objects.requireNonNull(
                actions,
                "Agent message actions are required."));
        if (actions.size() > 1
                || ("user".equals(role) && !actions.isEmpty())) {
            throw new IllegalArgumentException(
                    "Agent message actions are invalid.");
        }
        if (turnId != null) {
            turnId = DtoValidation.requireUuid(
                    turnId,
                    "Agent message turn ID");
        }
        createdAt = DtoValidation.requireInstant(
                createdAt,
                "Agent message creation");
    }
}
