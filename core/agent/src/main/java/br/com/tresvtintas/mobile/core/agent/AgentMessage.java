package br.com.tresvtintas.mobile.core.agent;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record AgentMessage(
        long id,
        AgentMessageRole role,
        String content,
        List<AgentDocument> documents,
        List<AgentAction> actions,
        Optional<String> turnId,
        Instant createdAt) {
    private static final int MAXIMUM_CONTENT_CHARS = 8_000;
    private static final int MINIMUM_ID = 1;

    public AgentMessage {
        if (id < MINIMUM_ID) {
            throw new IllegalArgumentException(
                    "Agent message ID is invalid.");
        }
        role = Objects.requireNonNull(
                role,
                "Agent message role is required.");
        if (content == null
                || content.length() > MAXIMUM_CONTENT_CHARS) {
            throw new IllegalArgumentException(
                    "Agent message content is invalid.");
        }
        documents = List.copyOf(Objects.requireNonNull(
                documents,
                "Agent message documents are required."));
        if (documents.size() > 1
                || (role == AgentMessageRole.USER
                && !documents.isEmpty())) {
            throw new IllegalArgumentException(
                    "Agent message documents are invalid.");
        }
        actions = List.copyOf(Objects.requireNonNull(
                actions,
                "Agent message actions are required."));
        if (actions.size() > 1
                || (role == AgentMessageRole.USER
                && !actions.isEmpty())) {
            throw new IllegalArgumentException(
                    "Agent message actions are invalid.");
        }
        turnId = Objects.requireNonNull(
                turnId,
                "Agent message turn is required.")
                .map(value -> AgentIdentifiers.requireUuid(
                        value,
                        "Agent message turn ID is invalid."));
        createdAt = Objects.requireNonNull(
                createdAt,
                "Agent message creation is required.");
    }
}
