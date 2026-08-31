package br.com.tresvtintas.mobile.core.agent;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record AgentMessagePage(
        String conversationId,
        List<AgentMessage> items,
        Optional<String> nextCursor) {
    public AgentMessagePage {
        conversationId = AgentIdentifiers.requireUuid(
                conversationId,
                "Agent message conversation ID is invalid.");
        if (items == null
                || items.size() > 100
                || items.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "Agent message page is invalid.");
        }
        items = List.copyOf(items);
        nextCursor = Objects.requireNonNull(
                nextCursor,
                "Agent message cursor is required.");
    }
}
