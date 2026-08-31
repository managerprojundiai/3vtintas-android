package br.com.tresvtintas.mobile.core.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.UUID;
import org.junit.Test;

public final class PersistedSessionTest {
    @Test
    public void evaluatesRefreshAndAbsoluteExpiry() {
        Instant now = Instant.parse("2026-07-25T12:00:00Z");
        PersistedSession session = session(now.plusSeconds(60), now.plusSeconds(120));

        assertTrue("Fresh persisted session must be usable.", session.isUsableAt(now));
        assertExpiredSessionIsRejected(session, now.plusSeconds(60));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsRefreshThatOutlivesSession() {
        Instant now = Instant.parse("2026-07-25T12:00:00Z");
        session(now.plusSeconds(121), now.plusSeconds(120));
    }

    static PersistedSession session(Instant refreshExpiry, Instant absoluteExpiry) {
        return new PersistedSession(
                "3vr1_" + "A".repeat(43),
                refreshExpiry,
                absoluteExpiry,
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString());
    }

    private static void assertExpiredSessionIsRejected(
            PersistedSession session, Instant now) {
        assertFalse(
                "Session at refresh expiry must not be usable.",
                session.isUsableAt(now));
    }
}
