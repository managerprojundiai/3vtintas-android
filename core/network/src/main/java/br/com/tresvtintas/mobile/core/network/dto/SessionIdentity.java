package br.com.tresvtintas.mobile.core.network.dto;

public record SessionIdentity(String id, String deviceId) {
    public SessionIdentity {
        id = DtoValidation.requireUuid(id, "Session ID");
        deviceId = DtoValidation.requireUuid(deviceId, "Device ID");
    }
}
