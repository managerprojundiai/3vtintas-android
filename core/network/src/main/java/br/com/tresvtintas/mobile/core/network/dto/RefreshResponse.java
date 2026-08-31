package br.com.tresvtintas.mobile.core.network.dto;

public record RefreshResponse(CredentialSet credentials) {
    public RefreshResponse {
        if (credentials == null) {
            throw new IllegalArgumentException("Credentials are required.");
        }
    }
}
