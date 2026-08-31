package br.com.tresvtintas.mobile.core.network.dto;

public record GoogleLoginResponse(AuthenticatedUser user, CredentialSet credentials) {
    public GoogleLoginResponse {
        if (user == null || credentials == null) {
            throw new IllegalArgumentException("User and credentials are required.");
        }
    }
}
