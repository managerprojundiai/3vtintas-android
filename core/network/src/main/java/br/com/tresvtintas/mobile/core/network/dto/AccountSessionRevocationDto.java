package br.com.tresvtintas.mobile.core.network.dto;

public record AccountSessionRevocationDto(
        String sessionId,
        boolean changed,
        boolean current) {
    public AccountSessionRevocationDto {
        sessionId = DtoValidation.requireUuid(
                sessionId,
                "Revoked account session ID");
    }
}
