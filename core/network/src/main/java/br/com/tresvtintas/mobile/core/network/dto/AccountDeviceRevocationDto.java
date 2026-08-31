package br.com.tresvtintas.mobile.core.network.dto;

public record AccountDeviceRevocationDto(
        String deviceId,
        boolean changed,
        boolean current) {
    public AccountDeviceRevocationDto {
        deviceId = DtoValidation.requireUuid(
                deviceId,
                "Revoked account device ID");
    }
}
