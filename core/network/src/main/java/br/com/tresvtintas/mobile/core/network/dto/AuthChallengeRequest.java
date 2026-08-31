package br.com.tresvtintas.mobile.core.network.dto;

public record AuthChallengeRequest(String installationId) {
    public AuthChallengeRequest {
        installationId = DtoValidation.requireUuid(installationId, "Installation ID");
    }
}
