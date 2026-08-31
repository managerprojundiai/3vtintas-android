package br.com.tresvtintas.mobile.core.auth;

import br.com.tresvtintas.mobile.core.network.BearerTokenRefresher;
import br.com.tresvtintas.mobile.core.network.dto.AuthChallengeResponse;
import br.com.tresvtintas.mobile.core.network.dto.CredentialSet;
import br.com.tresvtintas.mobile.core.network.dto.DeviceInfo;
import br.com.tresvtintas.mobile.core.network.dto.GoogleLoginRequest;
import br.com.tresvtintas.mobile.core.network.dto.GoogleLoginResponse;
import br.com.tresvtintas.mobile.core.network.dto.MeResponse;
import br.com.tresvtintas.mobile.core.network.dto.RefreshResponse;
import br.com.tresvtintas.mobile.core.network.dto.SessionIdentity;
import br.com.tresvtintas.mobile.core.security.PersistedSession;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Owns login, process restoration, refresh rotation and local/remote logout semantics.
 */
final class AuthSessionEngine implements BearerTokenRefresher {
    private final Object refreshLock = new Object();
    private final MobileAuthRemote publicRemote;
    private final Supplier<MobileAuthRemote> protectedRemote;
    private final SessionCredentialStore credentials;
    private final DeviceInfo device;
    private final Clock clock;

    AuthSessionEngine(
            MobileAuthRemote publicRemote,
            Supplier<MobileAuthRemote> protectedRemote,
            SessionCredentialStore credentials,
            DeviceInfo device,
            Clock clock) {
        this.publicRemote = Objects.requireNonNull(publicRemote, "Public auth remote is required.");
        this.protectedRemote = Objects.requireNonNull(
                protectedRemote, "Protected auth remote provider is required.");
        this.credentials = Objects.requireNonNull(credentials, "Credential store is required.");
        this.device = Objects.requireNonNull(device, "Device information is required.");
        this.clock = Objects.requireNonNull(clock, "Clock is required.");
    }

    AuthChallengeResponse beginLogin() throws AuthException {
        return publicRemote.createChallenge(device.installationId());
    }

    AuthenticatedSession completeLogin(
            AuthChallengeResponse challenge,
            String googleIdToken) throws AuthException {
        requireLiveChallenge(challenge);
        GoogleLoginResponse response = publicRemote.login(new GoogleLoginRequest(
                challenge.challengeId(),
                googleIdToken,
                device));
        credentials.replace(response.credentials());
        CredentialSet issued = response.credentials();
        return new AuthenticatedSession(
                response.user(),
                new SessionIdentity(issued.sessionId(), issued.deviceId()));
    }

    SessionRestoration restore() throws AuthException {
        if (credentials.usableSession().isEmpty()) {
            return SessionRestoration.signedOut();
        }
        Optional<String> refreshed = refreshSerialized(null);
        if (refreshed.isEmpty()) {
            return SessionRestoration.signedOut();
        }
        try {
            MeResponse response = protectedRemote.get().me();
            return SessionRestoration.authenticated(
                    new AuthenticatedSession(response.user(), response.session()));
        } catch (AuthException exception) {
            if (exception.kind() == AuthFailureKind.AUTH_REJECTED) {
                credentials.clear();
                return SessionRestoration.signedOut();
            }
            throw exception;
        }
    }

    LogoutResult logout() {
        boolean revoked = false;
        AuthFailureKind failure = null;
        try {
            if (refreshSerialized(null).isPresent()) {
                protectedRemote.get().logout();
                revoked = true;
            }
        } catch (AuthException exception) {
            failure = exception.kind();
        } finally {
            credentials.clear();
        }
        return new LogoutResult(revoked, Optional.ofNullable(failure));
    }

    void clearLocalSession() {
        credentials.clear();
    }

    @Override
    public Optional<String> refresh(String rejectedAccessToken) throws IOException {
        return refreshSerialized(rejectedAccessToken);
    }

    private Optional<String> refreshSerialized(String rejectedAccessToken) throws AuthException {
        synchronized (refreshLock) {
            Optional<String> current = credentials.accessToken();
            if (current.isPresent()
                    && (rejectedAccessToken == null
                    || !current.orElseThrow().equals(rejectedAccessToken))) {
                return current;
            }
            Optional<PersistedSession> persisted = credentials.usableSession();
            if (persisted.isEmpty()) {
                return Optional.empty();
            }
            try {
                RefreshResponse response = publicRemote.refresh(
                        persisted.orElseThrow().refreshToken());
                credentials.replace(response.credentials());
                return credentials.accessToken();
            } catch (AuthException exception) {
                if (exception.kind() == AuthFailureKind.AUTH_REJECTED) {
                    credentials.clear();
                    return Optional.empty();
                }
                throw exception;
            }
        }
    }

    private void requireLiveChallenge(AuthChallengeResponse challenge) throws AuthException {
        Objects.requireNonNull(challenge, "Authentication challenge is required.");
        Instant expiry = Instant.parse(challenge.expiresAt());
        if (!clock.instant().isBefore(expiry)) {
            throw new AuthException(
                    AuthFailureKind.AUTH_REJECTED,
                    "The authentication challenge expired before completion.");
        }
    }
}
