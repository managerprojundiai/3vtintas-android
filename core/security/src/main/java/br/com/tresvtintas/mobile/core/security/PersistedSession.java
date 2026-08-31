package br.com.tresvtintas.mobile.core.security;

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Long-lived session material that may be written only through the encrypted vault.
 */
public final class PersistedSession {
    private static final Pattern REFRESH_TOKEN =
            Pattern.compile("^3vr1_[A-Za-z0-9_-]{43}$");
    private final String refreshToken;
    private final Instant refreshTokenExpiresAt;
    private final Instant sessionAbsoluteExpiresAt;
    private final String sessionId;
    private final String deviceId;

    public PersistedSession(
            String refreshToken,
            Instant refreshTokenExpiresAt,
            Instant sessionAbsoluteExpiresAt,
            String sessionId,
            String deviceId) {
        if (refreshToken == null || !REFRESH_TOKEN.matcher(refreshToken).matches()) {
            throw new IllegalArgumentException("Refresh token does not match the mobile contract.");
        }
        this.refreshTokenExpiresAt = Objects.requireNonNull(
                refreshTokenExpiresAt, "Refresh token expiry is required.");
        this.sessionAbsoluteExpiresAt = Objects.requireNonNull(
                sessionAbsoluteExpiresAt, "Session absolute expiry is required.");
        if (refreshTokenExpiresAt.isAfter(sessionAbsoluteExpiresAt)) {
            throw new IllegalArgumentException(
                    "Refresh token cannot outlive the absolute session expiry.");
        }
        this.refreshToken = refreshToken;
        this.sessionId = UuidV4.require(sessionId, "Session ID");
        this.deviceId = UuidV4.require(deviceId, "Device ID");
    }

    public String refreshToken() {
        return refreshToken;
    }

    public Instant refreshTokenExpiresAt() {
        return refreshTokenExpiresAt;
    }

    public Instant sessionAbsoluteExpiresAt() {
        return sessionAbsoluteExpiresAt;
    }

    public String sessionId() {
        return sessionId;
    }

    public String deviceId() {
        return deviceId;
    }

    public boolean isUsableAt(Instant now) {
        Objects.requireNonNull(now, "Current time is required.");
        return now.isBefore(refreshTokenExpiresAt) && now.isBefore(sessionAbsoluteExpiresAt);
    }
}
