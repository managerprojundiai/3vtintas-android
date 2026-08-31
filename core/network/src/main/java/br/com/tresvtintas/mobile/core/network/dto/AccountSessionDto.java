package br.com.tresvtintas.mobile.core.network.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;

public record AccountSessionDto(
        @JsonProperty("id") String id,
        @JsonProperty("deviceId") String deviceId,
        @JsonProperty("authMethod") String authMethod,
        @JsonProperty("status") String status,
        @JsonProperty("issuedAt") String issuedAt,
        @JsonProperty("lastSeenAt") String lastSeenAt,
        @JsonProperty("idleExpiresAt") String idleExpiresAt,
        @JsonProperty("absoluteExpiresAt") String absoluteExpiresAt,
        @JsonProperty("endedAt") String endedAt,
        @JsonProperty("current") boolean current,
        @JsonProperty("revision") int revision) {
    private static final int MINIMUM_REVISION = 1;
    private static final Set<String> STATUSES =
            Set.of("active", "revoked", "expired");

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public AccountSessionDto {
        id = DtoValidation.requireUuid(id, "Account session ID");
        deviceId = DtoValidation.requireUuid(
                deviceId,
                "Account session device ID");
        if (!"google".equals(authMethod)) {
            throw new IllegalArgumentException(
                    "Account session authentication method is invalid.");
        }
        if (!STATUSES.contains(status)) {
            throw new IllegalArgumentException(
                    "Account session status is invalid.");
        }
        issuedAt = DtoValidation.requireInstant(
                issuedAt,
                "Account session issue time");
        lastSeenAt = DtoValidation.requireInstant(
                lastSeenAt,
                "Account session last seen time");
        idleExpiresAt = DtoValidation.requireInstant(
                idleExpiresAt,
                "Account session idle expiry");
        absoluteExpiresAt = DtoValidation.requireInstant(
                absoluteExpiresAt,
                "Account session absolute expiry");
        if (endedAt != null) {
            endedAt = DtoValidation.requireInstant(
                    endedAt,
                    "Account session end time");
        }
        if ("active".equals(status) && endedAt != null) {
            throw new IllegalArgumentException(
                    "Active account session cannot be ended.");
        }
        if ("revoked".equals(status) && endedAt == null) {
            throw new IllegalArgumentException(
                    "Revoked account session requires an end time.");
        }
        if (revision < MINIMUM_REVISION) {
            throw new IllegalArgumentException(
                    "Account session revision is invalid.");
        }
    }

    public AccountSessionDto(
            String id,
            String deviceId,
            String authMethod,
            String status,
            String issuedAt,
            String lastSeenAt,
            String idleExpiresAt,
            String absoluteExpiresAt,
            String endedAt,
            boolean current) {
        this(id, deviceId, authMethod, status, issuedAt, lastSeenAt,
                idleExpiresAt, absoluteExpiresAt, endedAt, current,
                MINIMUM_REVISION);
    }
}
