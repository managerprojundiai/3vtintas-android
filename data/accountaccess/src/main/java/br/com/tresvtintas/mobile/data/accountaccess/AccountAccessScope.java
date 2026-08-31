package br.com.tresvtintas.mobile.data.accountaccess;

import java.util.Objects;
import java.util.UUID;

public record AccountAccessScope(
        long userId,
        String sessionId,
        String deviceId,
        String authorizationRevision) {
    private static final long MINIMUM_USER_ID = 1L;

    public AccountAccessScope {
        if (userId < MINIMUM_USER_ID) {
            throw new IllegalArgumentException(
                    "Account access user ID is invalid.");
        }
        sessionId = uuid(sessionId, "Account access session ID");
        deviceId = uuid(deviceId, "Account access device ID");
        authorizationRevision = Objects.requireNonNull(
                authorizationRevision,
                "Account access authorization revision is required.");
        if (!authorizationRevision.matches("^[0-9a-f]{64}$")) {
            throw new IllegalArgumentException(
                    "Account access authorization revision is invalid.");
        }
    }

    private static String uuid(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " is required.");
        try {
            return UUID.fromString(value).toString();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    fieldName + " is not a UUID.",
                    exception);
        }
    }
}
