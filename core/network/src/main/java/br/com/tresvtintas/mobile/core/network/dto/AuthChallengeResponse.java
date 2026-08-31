package br.com.tresvtintas.mobile.core.network.dto;

public record AuthChallengeResponse(
        String challengeId,
        String nonce,
        String expiresAt,
        String googleServerClientId) {

    public AuthChallengeResponse {
        challengeId = DtoValidation.requireUuid(challengeId, "Challenge ID");
        if (nonce == null || !DtoValidation.NONCE.matcher(nonce).matches()) {
            throw new IllegalArgumentException("Challenge nonce is invalid.");
        }
        expiresAt = DtoValidation.requireInstant(expiresAt, "Challenge expiry");
        googleServerClientId = DtoValidation.requireText(
                googleServerClientId, "Google server client ID", 255);
    }
}
