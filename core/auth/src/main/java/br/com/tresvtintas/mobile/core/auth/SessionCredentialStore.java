package br.com.tresvtintas.mobile.core.auth;

import br.com.tresvtintas.mobile.core.network.BearerTokenProvider;
import br.com.tresvtintas.mobile.core.network.dto.CredentialSet;
import br.com.tresvtintas.mobile.core.security.AccessTokenMemoryStore;
import br.com.tresvtintas.mobile.core.security.PersistedSession;
import br.com.tresvtintas.mobile.core.security.SessionStorageException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.Optional;

/**
 * Commits rotating credentials in crash-safe order and exposes bearer state from memory only.
 */
public final class SessionCredentialStore implements BearerTokenProvider {
    private static final Duration MINIMUM_ACCESS_VALIDITY = Duration.ofSeconds(30);
    private final AccessTokenMemoryStore accessTokens;
    private final SessionVault sessionVault;
    private final Clock clock;

    SessionCredentialStore(
            AccessTokenMemoryStore accessTokens,
            SessionVault sessionVault,
            Clock clock) {
        this.accessTokens = Objects.requireNonNull(accessTokens, "Access token store is required.");
        this.sessionVault = Objects.requireNonNull(sessionVault, "Session vault is required.");
        this.clock = Objects.requireNonNull(clock, "Clock is required.");
    }

    public void replace(CredentialSet credentials) throws AuthException {
        Objects.requireNonNull(credentials, "Credentials are required.");
        PersistedSession persisted;
        Instant accessExpiry;
        try {
            persisted = new PersistedSession(
                    credentials.refreshToken(),
                    Instant.parse(credentials.refreshTokenExpiresAt()),
                    Instant.parse(credentials.sessionAbsoluteExpiresAt()),
                    credentials.sessionId(),
                    credentials.deviceId());
            accessExpiry = Instant.parse(credentials.accessTokenExpiresAt());
        } catch (DateTimeParseException | IllegalArgumentException exception) {
            throw new AuthException(
                    AuthFailureKind.PROTOCOL,
                    "The server returned an invalid credential set.",
                    exception);
        }
        try {
            sessionVault.save(persisted);
            accessTokens.replace(credentials.accessToken(), accessExpiry);
        } catch (SessionStorageException | IllegalArgumentException exception) {
            clear();
            throw new AuthException(
                    AuthFailureKind.STORAGE,
                    "The protected session could not be stored.",
                    exception);
        }
    }

    public Optional<PersistedSession> usableSession() throws AuthException {
        try {
            Optional<PersistedSession> loaded = sessionVault.load();
            if (loaded.isPresent() && !loaded.orElseThrow().isUsableAt(clock.instant())) {
                clear();
                return Optional.empty();
            }
            return loaded;
        } catch (SessionStorageException exception) {
            clear();
            throw new AuthException(
                    AuthFailureKind.STORAGE,
                    "The protected session could not be restored.",
                    exception);
        }
    }

    @Override
    public Optional<String> accessToken() {
        return accessTokens.current(clock.instant(), MINIMUM_ACCESS_VALIDITY);
    }

    public void clear() {
        accessTokens.clear();
        sessionVault.clear();
    }
}
