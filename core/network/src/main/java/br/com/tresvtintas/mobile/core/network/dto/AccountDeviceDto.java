package br.com.tresvtintas.mobile.core.network.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;

public record AccountDeviceDto(
        @JsonProperty("id") String id,
        @JsonProperty("displayName") String displayName,
        @JsonProperty("manufacturer") String manufacturer,
        @JsonProperty("model") String model,
        @JsonProperty("androidApi") int androidApi,
        @JsonProperty("appVersion") String appVersion,
        @JsonProperty("status") String status,
        @JsonProperty("registeredAt") String registeredAt,
        @JsonProperty("lastSeenAt") String lastSeenAt,
        @JsonProperty("revokedAt") String revokedAt,
        @JsonProperty("current") boolean current,
        @JsonProperty("revision") int revision) {
    private static final int MINIMUM_ANDROID_API = 26;
    private static final int MINIMUM_REVISION = 1;
    private static final Set<String> STATUSES =
            Set.of("active", "revoked");

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public AccountDeviceDto {
        id = DtoValidation.requireUuid(id, "Account device ID");
        displayName = DtoValidation.requireText(
                displayName,
                "Account device display name",
                120);
        manufacturer = DtoValidation.optionalText(
                manufacturer,
                "Account device manufacturer",
                80);
        model = DtoValidation.optionalText(
                model,
                "Account device model",
                120);
        if (androidApi < MINIMUM_ANDROID_API) {
            throw new IllegalArgumentException(
                    "Account device Android API is invalid.");
        }
        appVersion = DtoValidation.requireText(
                appVersion,
                "Account device app version",
                32);
        if (!STATUSES.contains(status)) {
            throw new IllegalArgumentException(
                    "Account device status is invalid.");
        }
        registeredAt = DtoValidation.requireInstant(
                registeredAt,
                "Account device registration time");
        lastSeenAt = DtoValidation.requireInstant(
                lastSeenAt,
                "Account device last seen time");
        if (revokedAt != null) {
            revokedAt = DtoValidation.requireInstant(
                    revokedAt,
                    "Account device revocation time");
        }
        if ("active".equals(status) && revokedAt != null) {
            throw new IllegalArgumentException(
                    "Active account device cannot be revoked.");
        }
        if ("revoked".equals(status) && revokedAt == null) {
            throw new IllegalArgumentException(
                    "Revoked account device requires a timestamp.");
        }
        if (revision < MINIMUM_REVISION) {
            throw new IllegalArgumentException(
                    "Account device revision is invalid.");
        }
    }

    public AccountDeviceDto(
            String id,
            String displayName,
            String manufacturer,
            String model,
            int androidApi,
            String appVersion,
            String status,
            String registeredAt,
            String lastSeenAt,
            String revokedAt,
            boolean current) {
        this(id, displayName, manufacturer, model, androidApi, appVersion,
                status, registeredAt, lastSeenAt, revokedAt, current,
                MINIMUM_REVISION);
    }
}
