package br.com.tresvtintas.mobile.core.network.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

public record AgentActionDecisionRequest(
        String decision,
        String confirmation,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String stepUpToken) {
    private static final Map<String, String> CONFIRMATIONS = Map.of(
            "confirm",
            "CONFIRM_AGENT_ACTION",
            "reject",
            "REJECT_AGENT_ACTION");

    public AgentActionDecisionRequest(
            String decision,
            String confirmation) {
        this(decision, confirmation, null);
    }

    public AgentActionDecisionRequest {
        if (!CONFIRMATIONS.getOrDefault(decision, "")
                        .equals(confirmation)
                || stepUpToken != null
                        && (!"confirm".equals(decision)
                                || !stepUpToken.matches(
                                        "^3vsu1_[A-Za-z0-9_-]{43}$"))) {
            throw new IllegalArgumentException(
                    "Agent action decision is invalid.");
        }
    }
}
