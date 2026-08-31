package br.com.tresvtintas.mobile.core.network.dto;

public record AgentStepUpChallengeResponse(
        String challengeId,
        String nonce,
        String googleServerClientId,
        String expiresAt) {
    public AgentStepUpChallengeResponse {
        challengeId = DtoValidation.requireUuid(
                challengeId,
                "Agent step-up challenge ID");
        if (nonce == null
                || !DtoValidation.NONCE.matcher(nonce).matches()) {
            throw new IllegalArgumentException(
                    "Agent step-up nonce is invalid.");
        }
        googleServerClientId = DtoValidation.requireText(
                googleServerClientId,
                "Agent step-up Google client ID",
                255);
        expiresAt = DtoValidation.requireInstant(
                expiresAt,
                "Agent step-up challenge expiry");
    }
}
