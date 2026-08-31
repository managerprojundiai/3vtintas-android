package br.com.tresvtintas.mobile.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import br.com.tresvtintas.mobile.core.security.EncryptedSessionStore;
import br.com.tresvtintas.mobile.core.security.InstallationIdentityStore;
import br.com.tresvtintas.mobile.core.security.PersistedSession;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class SecureSessionInstrumentedTest {
    private static final String TEST_NAMESPACE = "instrumentation";

    @Test
    public void installationIdentityIsStableUuidV4() {
        Context context = ApplicationProvider.getApplicationContext();
        InstallationIdentityStore store = new InstallationIdentityStore(context);

        String first = store.getOrCreate();
        String second = store.getOrCreate();

        assertEquals("Installation identity must be stable.", first, second);
        assertUuidV4(first);
    }

    @Test
    public void keystoreVaultRoundTripsAndClearsIsolatedSession() {
        Context context = ApplicationProvider.getApplicationContext();
        EncryptedSessionStore store = new EncryptedSessionStore(context, TEST_NAMESPACE);
        PersistedSession expected = session();
        store.clear();

        store.save(expected);
        Optional<PersistedSession> loaded = store.load();

        assertTrue("Isolated encrypted session must load.", loaded.isPresent());
        assertLoadedSession(expected, loaded.orElseThrow());
        store.clear();
        assertVaultIsEmpty(store);
    }

    private static PersistedSession session() {
        return new PersistedSession(
                "3vr1_" + "A".repeat(43),
                Instant.parse("2026-08-01T12:00:00Z"),
                Instant.parse("2026-08-20T12:00:00Z"),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString());
    }

    private static void assertUuidV4(String value) {
        assertEquals(
                "Installation identity must be a UUID v4.",
                4,
                UUID.fromString(value).version());
    }

    private static void assertLoadedSession(
            PersistedSession expected, PersistedSession actual) {
        assertEquals(
                "Keystore vault must recover the refresh token.",
                expected.refreshToken(),
                actual.refreshToken());
        assertEquals(
                "Keystore vault must recover the session ID.",
                expected.sessionId(),
                actual.sessionId());
    }

    private static void assertVaultIsEmpty(EncryptedSessionStore store) {
        assertFalse("Explicit clear must remove the encrypted session.", store.load().isPresent());
    }
}
