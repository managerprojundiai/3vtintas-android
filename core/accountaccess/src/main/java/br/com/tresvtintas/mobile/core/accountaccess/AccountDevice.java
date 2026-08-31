package br.com.tresvtintas.mobile.core.accountaccess;

import java.time.Instant;
import java.util.Optional;

public record AccountDevice(
        String id,
        String displayName,
        Optional<String> manufacturer,
        Optional<String> model,
        int androidApi,
        String appVersion,
        AccountAccessStatus status,
        Instant registeredAt,
        Instant lastSeenAt,
        Optional<Instant> revokedAt,
        boolean current,
        int revision) implements AccountAccessEntry {
    private static final int MINIMUM_ANDROID_API = 26;
    private static final int MINIMUM_REVISION = 1;

    public AccountDevice {
        id = AccountAccessValidation.uuid(id, "Device ID");
        displayName = AccountAccessValidation.text(
                displayName,
                "Device display name",
                120);
        manufacturer = AccountAccessValidation.optionalText(
                manufacturer,
                "Device manufacturer",
                80);
        model = AccountAccessValidation.optionalText(
                model,
                "Device model",
                120);
        if (androidApi < MINIMUM_ANDROID_API) {
            throw new IllegalArgumentException(
                    "Device Android API is invalid.");
        }
        appVersion = AccountAccessValidation.text(
                appVersion,
                "Device app version",
                32);
        if (status == null || status == AccountAccessStatus.EXPIRED) {
            throw new IllegalArgumentException(
                    "Device status is invalid.");
        }
        if (registeredAt == null || lastSeenAt == null) {
            throw new IllegalArgumentException(
                    "Device timestamps are required.");
        }
        if (lastSeenAt.isBefore(registeredAt)) {
            throw new IllegalArgumentException(
                    "Device activity cannot predate registration.");
        }
        revokedAt = revokedAt == null ? Optional.empty() : revokedAt;
        if (status == AccountAccessStatus.ACTIVE && revokedAt.isPresent()) {
            throw new IllegalArgumentException(
                    "Active device cannot have a revocation timestamp.");
        }
        if (status == AccountAccessStatus.REVOKED && revokedAt.isEmpty()) {
            throw new IllegalArgumentException(
                    "Revoked device requires a revocation timestamp.");
        }
        if (revokedAt.filter(value ->
                        value.isBefore(registeredAt))
                .isPresent()) {
            throw new IllegalArgumentException(
                    "Device revocation cannot predate registration.");
        }
        if (current && status != AccountAccessStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Current device must be active.");
        }
        if (revision < MINIMUM_REVISION) {
            throw new IllegalArgumentException(
                    "Device revision is invalid.");
        }
    }

    public AccountDevice(
            String id,
            String displayName,
            Optional<String> manufacturer,
            Optional<String> model,
            int androidApi,
            String appVersion,
            AccountAccessStatus status,
            Instant registeredAt,
            Instant lastSeenAt,
            Optional<Instant> revokedAt,
            boolean current) {
        this(id, displayName, manufacturer, model, androidApi, appVersion,
                status, registeredAt, lastSeenAt, revokedAt, current,
                MINIMUM_REVISION);
    }
}
