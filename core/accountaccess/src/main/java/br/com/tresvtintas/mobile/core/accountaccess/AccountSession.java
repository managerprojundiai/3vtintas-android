package br.com.tresvtintas.mobile.core.accountaccess;

import java.time.Instant;
import java.util.Optional;

public record AccountSession(
        String id,
        String deviceId,
        AccountAuthMethod authMethod,
        AccountAccessStatus status,
        Instant issuedAt,
        Instant lastSeenAt,
        Instant idleExpiresAt,
        Instant absoluteExpiresAt,
        Optional<Instant> endedAt,
        boolean current,
        int revision) implements AccountAccessEntry {
    private static final int MINIMUM_REVISION = 1;

    public AccountSession {
        id = AccountAccessValidation.uuid(id, "Session ID");
        deviceId = AccountAccessValidation.uuid(
                deviceId,
                "Session device ID");
        if (authMethod == null || status == null) {
            throw new IllegalArgumentException(
                    "Session method and status are required.");
        }
        if (issuedAt == null
                || lastSeenAt == null
                || idleExpiresAt == null
                || absoluteExpiresAt == null) {
            throw new IllegalArgumentException(
                    "Session timestamps are required.");
        }
        if (lastSeenAt.isBefore(issuedAt)
                || !idleExpiresAt.isAfter(issuedAt)
                || !absoluteExpiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException(
                    "Session timestamp order is invalid.");
        }
        endedAt = endedAt == null ? Optional.empty() : endedAt;
        if (status == AccountAccessStatus.ACTIVE && endedAt.isPresent()) {
            throw new IllegalArgumentException(
                    "Active session cannot have an end timestamp.");
        }
        if (status == AccountAccessStatus.REVOKED && endedAt.isEmpty()) {
            throw new IllegalArgumentException(
                    "Revoked session requires an end timestamp.");
        }
        if (endedAt.filter(value -> value.isBefore(issuedAt))
                .isPresent()) {
            throw new IllegalArgumentException(
                    "Session end cannot predate its issue.");
        }
        if (current && status != AccountAccessStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Current session must be active.");
        }
        if (revision < MINIMUM_REVISION) {
            throw new IllegalArgumentException(
                    "Session revision is invalid.");
        }
    }

    public AccountSession(
            String id,
            String deviceId,
            AccountAuthMethod authMethod,
            AccountAccessStatus status,
            Instant issuedAt,
            Instant lastSeenAt,
            Instant idleExpiresAt,
            Instant absoluteExpiresAt,
            Optional<Instant> endedAt,
            boolean current) {
        this(id, deviceId, authMethod, status, issuedAt, lastSeenAt,
                idleExpiresAt, absoluteExpiresAt, endedAt, current,
                MINIMUM_REVISION);
    }
}
