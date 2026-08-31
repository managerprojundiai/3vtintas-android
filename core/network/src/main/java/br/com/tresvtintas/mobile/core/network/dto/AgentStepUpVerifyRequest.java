package br.com.tresvtintas.mobile.core.network.dto;

public record AgentStepUpVerifyRequest(
        String challengeId,
        String credential) {
    private static final int MINIMUM_CREDENTIAL_LENGTH = 80;

    public AgentStepUpVerifyRequest {
        challengeId = DtoValidation.requireUuid(
                challengeId,
                "Agent step-up challenge ID");
        credential = DtoValidation.requireText(
                credential,
                "Agent step-up credential",
                8_192);
        if (credential.length() < MINIMUM_CREDENTIAL_LENGTH) {
            throw new IllegalArgumentException(
                    "Agent step-up credential is invalid.");
        }
    }
}
