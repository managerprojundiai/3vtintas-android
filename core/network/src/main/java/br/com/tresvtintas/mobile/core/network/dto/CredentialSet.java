package br.com.tresvtintas.mobile.core.network.dto;

public record CredentialSet(
        String accessToken,
        String accessTokenExpiresAt,
        String refreshToken,
        String refreshTokenExpiresAt,
        String sessionAbsoluteExpiresAt,
        String sessionId,
        String deviceId) {
    private static final int MINIMUM_ACCESS_TOKEN_LENGTH = 80;

    public CredentialSet {
        accessToken = DtoValidation.requireText(accessToken, "Access token", 4096);
        if (accessToken.length() < MINIMUM_ACCESS_TOKEN_LENGTH) {
            throw new IllegalArgumentException("Access token is too short.");
        }
        accessTokenExpiresAt = DtoValidation.requireInstant(
                accessTokenExpiresAt, "Access token expiry");
        if (refreshToken == null || !DtoValidation.REFRESH_TOKEN.matcher(refreshToken).matches()) {
            throw new IllegalArgumentException("Refresh token is invalid.");
        }
        refreshTokenExpiresAt = DtoValidation.requireInstant(
                refreshTokenExpiresAt, "Refresh token expiry");
        sessionAbsoluteExpiresAt = DtoValidation.requireInstant(
                sessionAbsoluteExpiresAt, "Session absolute expiry");
        sessionId = DtoValidation.requireUuid(sessionId, "Session ID");
        deviceId = DtoValidation.requireUuid(deviceId, "Device ID");
    }
}
