package br.com.tresvtintas.mobile.core.network.dto;

public record AgentStepUpGrantResponse(
        String stepUpToken,
        String expiresAt) {
    public AgentStepUpGrantResponse {
        if (stepUpToken == null
                || !stepUpToken.matches(
                        "^3vsu1_[A-Za-z0-9_-]{43}$")) {
            throw new IllegalArgumentException(
                    "Agent step-up grant is invalid.");
        }
        expiresAt = DtoValidation.requireInstant(
                expiresAt,
                "Agent step-up grant expiry");
    }
}
