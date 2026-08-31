package br.com.tresvtintas.mobile.core.network.dto;

public record MeResponse(AuthenticatedUser user, SessionIdentity session) {
    public MeResponse {
        if (user == null || session == null) {
            throw new IllegalArgumentException("User and session are required.");
        }
    }
}
