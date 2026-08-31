package br.com.tresvtintas.mobile.core.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.network.dto.AuthChallengeResponse;
import br.com.tresvtintas.mobile.core.security.AccessTokenMemoryStore;
import br.com.tresvtintas.mobile.core.security.PersistedSession;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.Before;
import org.junit.Test;

public final class AuthSessionEngineTest {
    private FakeSessionVault vault;
    private SessionCredentialStore credentialStore;
    private FakeMobileAuthRemote remote;
    private AuthSessionEngine engine;

    @Before
    public void setUp() {
        vault = new FakeSessionVault();
        credentialStore = new SessionCredentialStore(
                new AccessTokenMemoryStore(),
                vault,
                Clock.fixed(AuthTestFixtures.NOW, ZoneOffset.UTC));
        remote = new FakeMobileAuthRemote();
        engine = new AuthSessionEngine(
                remote,
                () -> remote,
                credentialStore,
                AuthTestFixtures.device(),
                Clock.fixed(AuthTestFixtures.NOW, ZoneOffset.UTC));
    }

    @Test
    public void restoresAfterProcessDeathByRotatingThenResolvingMe() throws Exception {
        vault.save(persisted(AuthTestFixtures.REFRESH_TOKEN_A));

        SessionRestoration restoration = engine.restore();

        assertTrue("Durable session must restore silently.", restoration.session().isPresent());
        assertEquals(AuthTestFixtures.user(), restoration.session().orElseThrow().user());
        assertEquals("Process restoration must rotate once.", 1, remote.refreshCalls.get());
        assertEquals(
                "Rotated access token must return to process memory.",
                AuthTestFixtures.ACCESS_TOKEN_B,
                credentialStore.accessToken().orElseThrow());
    }

    @Test
    public void serializesConcurrentRefreshOfTheSameRejectedToken() throws Exception {
        credentialStore.replace(AuthTestFixtures.credentials(
                AuthTestFixtures.ACCESS_TOKEN_A,
                AuthTestFixtures.REFRESH_TOKEN_A));
        int callers = 12;
        ExecutorService executor = Executors.newFixedThreadPool(callers);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Optional<String>>> futures = new ArrayList<>();
        try {
            for (int index = 0; index < callers; index++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return engine.refresh(AuthTestFixtures.ACCESS_TOKEN_A);
                }));
            }
            start.countDown();
            for (Future<Optional<String>> future : futures) {
                assertEquals(AuthTestFixtures.ACCESS_TOKEN_B, future.get().orElseThrow());
            }
        } finally {
            executor.shutdownNow();
        }

        assertEquals(
                "All rejected requests must share one refresh rotation.",
                1,
                remote.refreshCalls.get());
    }

    @Test
    public void clearsSessionWhenRefreshIsRejected() throws Exception {
        credentialStore.replace(AuthTestFixtures.credentials(
                AuthTestFixtures.ACCESS_TOKEN_A,
                AuthTestFixtures.REFRESH_TOKEN_A));
        remote.refreshFailure = new AuthException(
                AuthFailureKind.AUTH_REJECTED,
                "forced");

        Optional<String> refreshed = engine.refresh(AuthTestFixtures.ACCESS_TOKEN_A);

        assertTrue("Rejected refresh cannot return a bearer.", refreshed.isEmpty());
        assertTrue("Rejected refresh must remove process token.",
                credentialStore.accessToken().isEmpty());
        assertTrue("Rejected refresh must remove durable session.", vault.load().isEmpty());
    }

    @Test
    public void logoutAlwaysClearsLocalStateWhenNetworkIsUnavailable() throws Exception {
        credentialStore.replace(AuthTestFixtures.credentials(
                AuthTestFixtures.ACCESS_TOKEN_A,
                AuthTestFixtures.REFRESH_TOKEN_A));
        remote.logoutFailure = new AuthException(AuthFailureKind.NETWORK, "forced");

        LogoutResult result = engine.logout();

        assertFalse("Remote revocation was not confirmed.", result.remotelyRevoked());
        assertEquals(AuthFailureKind.NETWORK, result.remoteFailure().orElseThrow());
        assertTrue("Logout must clear the access token.", credentialStore.accessToken().isEmpty());
        assertTrue("Logout must clear the refresh vault.", vault.load().isEmpty());
    }

    @Test
    public void completesNonceBoundGoogleLoginAndCommitsCredentials() throws Exception {
        AuthChallengeResponse challenge = remote.challenge;

        AuthenticatedSession session = engine.completeLogin(
                challenge,
                "g".repeat(80));

        assertEquals(AuthTestFixtures.user(), session.user());
        assertEquals(challenge.challengeId(), remote.capturedLogin.challengeId());
        assertEquals(AuthTestFixtures.device(), remote.capturedLogin.device());
        assertEquals(
                AuthTestFixtures.REFRESH_TOKEN_A,
                vault.load().orElseThrow().refreshToken());
    }

    @Test
    public void refusesExpiredChallengeBeforeSendingCredential() {
        AuthChallengeResponse expired = new AuthChallengeResponse(
                "550e8400-e29b-41d4-a716-446655440003",
                "3vn1_" + "n".repeat(43),
                AuthTestFixtures.NOW.toString(),
                "client.apps.googleusercontent.com");

        AuthException failure = assertThrows(
                AuthException.class,
                () -> engine.completeLogin(expired, "g".repeat(80)));

        assertEquals(AuthFailureKind.AUTH_REJECTED, failure.kind());
        assertTrue("Expired challenge must not reach login endpoint.",
                remote.capturedLogin == null);
    }

    private static PersistedSession persisted(String refreshToken) {
        return new PersistedSession(
                refreshToken,
                AuthTestFixtures.NOW.plusSeconds(86_400),
                AuthTestFixtures.NOW.plusSeconds(172_800),
                AuthTestFixtures.SESSION_ID,
                AuthTestFixtures.DEVICE_ID);
    }
}
