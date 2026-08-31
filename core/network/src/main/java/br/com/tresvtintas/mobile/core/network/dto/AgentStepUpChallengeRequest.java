package br.com.tresvtintas.mobile.core.network.dto;

public record AgentStepUpChallengeRequest(String confirmation) {
    private static final String CONFIRMATION =
            "REQUEST_AGENT_ACTION_STEP_UP";

    public AgentStepUpChallengeRequest {
        if (!CONFIRMATION.equals(confirmation)) {
            throw new IllegalArgumentException(
                    "Agent step-up request is invalid.");
        }
    }

    public static AgentStepUpChallengeRequest confirmed() {
        return new AgentStepUpChallengeRequest(CONFIRMATION);
    }
}
