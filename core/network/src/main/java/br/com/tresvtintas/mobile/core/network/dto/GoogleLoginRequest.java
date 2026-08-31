package br.com.tresvtintas.mobile.core.network.dto;

public record GoogleLoginRequest(
        String challengeId,
        String credential,
        DeviceInfo device) {
    private static final int MINIMUM_CREDENTIAL_LENGTH = 80;

    public GoogleLoginRequest {
        challengeId = DtoValidation.requireUuid(challengeId, "Challenge ID");
        credential = DtoValidation.requireText(credential, "Google credential", 8192);
        if (credential.length() < MINIMUM_CREDENTIAL_LENGTH) {
            throw new IllegalArgumentException("Google credential is too short.");
        }
        if (device == null) {
            throw new IllegalArgumentException("Device information is required.");
        }
    }
}
