package br.com.tresvtintas.mobile.core.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.time.Instant;
import org.junit.Test;

public final class AccessTokenMemoryStoreTest {
    private static final String TOKEN = "a".repeat(80);

    @Test
    public void returnsTokenOnlyWhileMinimumValidityRemains() {
        AccessTokenMemoryStore store = new AccessTokenMemoryStore();
        Instant now = Instant.parse("2026-07-25T12:00:00Z");
        store.replace(TOKEN, now.plusSeconds(120));

        assertEquals(
                "Token must be available while sufficient validity remains.",
                TOKEN,
                store.current(now, Duration.ofSeconds(30)).orElseThrow());
        assertExpiredTokenIsCleared(store, now.plusSeconds(91));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsShortToken() {
        new AccessTokenMemoryStore().replace("short", Instant.now().plusSeconds(60));
    }

    private static void assertExpiredTokenIsCleared(
            AccessTokenMemoryStore store, Instant now) {
        assertTrue(
                "Token inside the safety window must be rejected.",
                store.current(now, Duration.ofSeconds(30)).isEmpty());
        assertTrue(
                "Rejected token must be removed from memory.",
                store.current(now, Duration.ZERO).isEmpty());
    }
}
