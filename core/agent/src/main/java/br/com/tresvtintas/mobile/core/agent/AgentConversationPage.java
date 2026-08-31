package br.com.tresvtintas.mobile.core.agent;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record AgentConversationPage(
        List<AgentConversation> items,
        Optional<String> nextCursor) {
    public AgentConversationPage {
        if (items == null
                || items.size() > 100
                || items.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "Agent conversation page is invalid.");
        }
        items = List.copyOf(items);
        nextCursor = Objects.requireNonNull(
                nextCursor,
                "Agent conversation cursor is required.");
    }
}
