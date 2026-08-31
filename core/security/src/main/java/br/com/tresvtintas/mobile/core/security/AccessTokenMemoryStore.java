package br.com.tresvtintas.mobile.core.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Process-memory-only holder for short-lived bearer credentials.
 */
public final class AccessTokenMemoryStore {
    private static final int MINIMUM_TOKEN_LENGTH = 80;
    private static final int MAXIMUM_TOKEN_LENGTH = 4096;
    private Optional<TokenState> state = Optional.empty();

    public synchronized void replace(String accessToken, Instant accessTokenExpiresAt) {
        if (accessToken == null
                || accessToken.length() < MINIMUM_TOKEN_LENGTH
                || accessToken.length() > MAXIMUM_TOKEN_LENGTH) {
            throw new IllegalArgumentException("Access token has an invalid length.");
        }
        Instant expiry = Objects.requireNonNull(
                accessTokenExpiresAt, "Access token expiry is required.");
        this.state = Optional.of(new TokenState(accessToken, expiry));
    }

    public synchronized Optional<String> current(Instant now, Duration minimumValidity) {
        Objects.requireNonNull(now, "Current time is required.");
        Objects.requireNonNull(minimumValidity, "Minimum validity is required.");
        if (minimumValidity.isNegative()) {
            throw new IllegalArgumentException("Minimum validity cannot be negative.");
        }
        if (state.isEmpty()
                || !now.plus(minimumValidity).isBefore(state.orElseThrow().expiresAt())) {
            clear();
            return Optional.empty();
        }
        return Optional.of(state.orElseThrow().token());
    }

    public synchronized void clear() {
        state = Optional.empty();
    }

    private record TokenState(String token, Instant expiresAt) {
    }
}
