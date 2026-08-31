package br.com.tresvtintas.mobile.core.network.dto;

public record AgentCancelTurnRequest(String confirmation) {
    public static final String REQUIRED_CONFIRMATION =
            "CANCEL_AGENT_TURN";

    public AgentCancelTurnRequest {
        if (!REQUIRED_CONFIRMATION.equals(confirmation)) {
            throw new IllegalArgumentException(
                    "Agent cancellation confirmation is invalid.");
        }
    }

    public static AgentCancelTurnRequest confirmed() {
        return new AgentCancelTurnRequest(REQUIRED_CONFIRMATION);
    }
}
