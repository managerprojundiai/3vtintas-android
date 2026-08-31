package br.com.tresvtintas.mobile.core.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.security.AccessTokenMemoryStore;
import br.com.tresvtintas.mobile.core.security.SessionStorageException;
import java.time.Clock;
import java.time.ZoneOffset;
import org.junit.Before;
import org.junit.Test;

public final class SessionCredentialStoreTest {
    private FakeSessionVault vault;
    private SessionCredentialStore store;

    @Before
    public void setUp() {
        vault = new FakeSessionVault();
        store = new SessionCredentialStore(
                new AccessTokenMemoryStore(),
                vault,
                Clock.fixed(AuthTestFixtures.NOW, ZoneOffset.UTC));
    }

    @Test
    public void commitsRefreshBeforePublishingAccessToken() throws Exception {
        store.replace(AuthTestFixtures.credentials(
                AuthTestFixtures.ACCESS_TOKEN_A,
                AuthTestFixtures.REFRESH_TOKEN_A));

        assertEquals(
                "Access token must be available only from process memory.",
                AuthTestFixtures.ACCESS_TOKEN_A,
                store.accessToken().orElseThrow());
        assertEquals(
                "Refresh token must be stored in the protected vault.",
                AuthTestFixtures.REFRESH_TOKEN_A,
                store.usableSession().orElseThrow().refreshToken());
    }

    @Test
    public void clearsAllCredentialStateWhenProtectedCommitFails() {
        vault.failSave(new SessionStorageException("forced"));

        AuthException failure = assertThrows(
                AuthException.class,
                () -> store.replace(AuthTestFixtures.credentials(
                        AuthTestFixtures.ACCESS_TOKEN_A,
                        AuthTestFixtures.REFRESH_TOKEN_A)));

        assertEquals(AuthFailureKind.STORAGE, failure.kind());
        assertTrue("Access token must remain absent.", store.accessToken().isEmpty());
        assertTrue("Vault must be empty after failed rotation.", vault.load().isEmpty());
    }

    @Test
    public void failsClosedWhenEncryptedSessionCannotBeRead() {
        vault.failLoad(new SessionStorageException("forced"));

        AuthException failure = assertThrows(AuthException.class, store::usableSession);

        assertEquals(AuthFailureKind.STORAGE, failure.kind());
        assertFalse("Failure must not invent an access token.", store.accessToken().isPresent());
    }
}
