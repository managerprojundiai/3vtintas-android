package br.com.tresvtintas.mobile.core.network.dto;

public record RefreshRequest(String refreshToken) {
    public RefreshRequest {
        if (refreshToken == null || !DtoValidation.REFRESH_TOKEN.matcher(refreshToken).matches()) {
            throw new IllegalArgumentException("Refresh token is invalid.");
        }
    }
}
