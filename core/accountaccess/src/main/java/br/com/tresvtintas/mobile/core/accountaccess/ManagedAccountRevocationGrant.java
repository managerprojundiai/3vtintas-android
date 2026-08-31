package br.com.tresvtintas.mobile.core.accountaccess;

import java.time.Instant;
import java.util.Objects;

public record ManagedAccountRevocationGrant(
        String token,
        Instant expiresAt) {
    public ManagedAccountRevocationGrant {
        if (token == null
                || !token.matches("^3vsu1_[A-Za-z0-9_-]{43}$")) {
            throw new IllegalArgumentException(
                    "Managed revocation grant is invalid.");
        }
        expiresAt = Objects.requireNonNull(
                expiresAt,
                "Managed revocation grant expiry is required.");
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(Objects.requireNonNull(
                now,
                "Managed revocation clock is required."));
    }
}
