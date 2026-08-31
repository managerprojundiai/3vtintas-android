package br.com.tresvtintas.mobile.core.agent;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class AgentPendingTurn {
    private AgentPendingTurn() {
        throw new AssertionError("No instances.");
    }

    /**
     * Finds the newest user turn without a public assistant response. This lets the client recover
     * a durable queued/running turn after process recreation without storing message content.
     */
    public static Optional<String> from(List<AgentMessage> messages) {
        if (messages == null) {
            throw new IllegalArgumentException(
                    "Agent messages are required.");
        }
        Set<String> answered = new HashSet<>();
        for (AgentMessage message : messages) {
            if (message == null) {
                throw new IllegalArgumentException(
                        "Agent message is required.");
            }
            if (message.role() == AgentMessageRole.ASSISTANT) {
                message.turnId().ifPresent(answered::add);
            }
        }
        for (int index = messages.size() - 1; index >= 0; index--) {
            AgentMessage message = messages.get(index);
            if (message.role() == AgentMessageRole.USER
                    && message.turnId().isPresent()
                    && !answered.contains(
                            message.turnId().orElseThrow())) {
                return message.turnId();
            }
        }
        return Optional.empty();
    }
}
