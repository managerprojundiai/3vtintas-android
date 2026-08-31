package br.com.tresvtintas.mobile.core.network.dto;

public record PushRegistrationResponse(
        String provider,
        boolean registered,
        boolean changed,
        String updatedAt) {

    public PushRegistrationResponse {
        if (!"fcm".equals(provider)) {
            throw new IllegalArgumentException(
                    "Push registration provider is invalid.");
        }
        updatedAt = DtoValidation.requireInstant(
                updatedAt,
                "Push registration update time");
    }
}
