package br.com.tresvtintas.mobile.core.accountaccess;

import java.time.Instant;
import java.util.Objects;

public record ManagedAccountRevocationChallenge(
        String id,
        String nonce,
        String googleServerClientId,
        Instant expiresAt) {
    public ManagedAccountRevocationChallenge {
        id = AccountAccessValidation.uuid(
                id,
                "Managed revocation challenge ID");
        if (nonce == null
                || !nonce.matches("^3vn1_[A-Za-z0-9_-]{43}$")) {
            throw new IllegalArgumentException(
                    "Managed revocation nonce is invalid.");
        }
        googleServerClientId = AccountAccessValidation.text(
                googleServerClientId,
                "Managed revocation Google client ID",
                255);
        expiresAt = Objects.requireNonNull(
                expiresAt,
                "Managed revocation challenge expiry is required.");
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(Objects.requireNonNull(
                now,
                "Managed revocation clock is required."));
    }
}
