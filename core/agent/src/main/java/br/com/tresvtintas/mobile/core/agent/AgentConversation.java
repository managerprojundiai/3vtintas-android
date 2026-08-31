package br.com.tresvtintas.mobile.core.agent;

import java.time.Instant;
import java.util.Objects;

public record AgentConversation(
        String id,
        String title,
        AgentConversationStatus status,
        boolean createdOnThisDevice,
        Instant lastActivityAt,
        Instant createdAt,
        Instant updatedAt) {
    private static final int MAXIMUM_TITLE_CHARS = 120;

    public AgentConversation {
        id = AgentIdentifiers.requireUuid(
                id,
                "Agent conversation ID is invalid.");
        if (title == null
                || title.isBlank()
                || title.length() > MAXIMUM_TITLE_CHARS) {
            throw new IllegalArgumentException(
                    "Agent conversation title is invalid.");
        }
        status = Objects.requireNonNull(
                status,
                "Agent conversation status is required.");
        lastActivityAt = Objects.requireNonNull(
                lastActivityAt,
                "Agent conversation activity is required.");
        createdAt = Objects.requireNonNull(
                createdAt,
                "Agent conversation creation is required.");
        updatedAt = Objects.requireNonNull(
                updatedAt,
                "Agent conversation update is required.");
    }
}
