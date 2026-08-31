package br.com.tresvtintas.mobile.core.agent;

import java.time.Instant;
import java.util.Objects;

public record AgentStepUpChallenge(
        String id,
        String nonce,
        String googleServerClientId,
        Instant expiresAt) {
    public AgentStepUpChallenge {
        id = AgentIdentifiers.requireUuid(
                id,
                "Agent step-up challenge ID is invalid.");
        if (nonce == null
                || !nonce.matches("^3vn1_[A-Za-z0-9_-]{43}$")) {
            throw new IllegalArgumentException(
                    "Agent step-up nonce is invalid.");
        }
        if (googleServerClientId == null
                || googleServerClientId.isBlank()
                || !googleServerClientId.equals(
                        googleServerClientId.trim())
                || googleServerClientId.length() > 255) {
            throw new IllegalArgumentException(
                    "Agent step-up Google client ID is invalid.");
        }
        expiresAt = Objects.requireNonNull(
                expiresAt,
                "Agent step-up challenge expiry is required.");
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(Objects.requireNonNull(
                now,
                "Agent step-up clock is required."));
    }
}
