package br.com.tresvtintas.mobile.core.agent;

import java.time.Instant;
import java.util.Objects;

public record AgentStepUpGrant(
        String token,
        Instant expiresAt) {
    public AgentStepUpGrant {
        if (token == null
                || !token.matches("^3vsu1_[A-Za-z0-9_-]{43}$")) {
            throw new IllegalArgumentException(
                    "Agent step-up token is invalid.");
        }
        expiresAt = Objects.requireNonNull(
                expiresAt,
                "Agent step-up grant expiry is required.");
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(Objects.requireNonNull(
                now,
                "Agent step-up clock is required."));
    }
}
