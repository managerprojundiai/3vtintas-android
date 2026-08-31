package br.com.tresvtintas.mobile.core.agent;

import java.util.Objects;

public record AgentActionDecisionResult(
        AgentAction action,
        boolean replayed) {
    public AgentActionDecisionResult {
        action = Objects.requireNonNull(
                action,
                "Agent action decision result is required.");
        if (!action.status().terminal()) {
            throw new IllegalArgumentException(
                    "Agent action decision is not terminal.");
        }
    }
}
