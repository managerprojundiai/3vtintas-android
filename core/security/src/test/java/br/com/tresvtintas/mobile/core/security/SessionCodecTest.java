package br.com.tresvtintas.mobile.core.security;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import org.junit.Test;

public final class SessionCodecTest {
    @Test
    public void roundTripsVersionedSessionPayload() {
        PersistedSession expected = PersistedSessionTest.session(
                Instant.parse("2026-08-01T12:00:00Z"),
                Instant.parse("2026-08-20T12:00:00Z"));
        SessionCodec codec = new SessionCodec();

        PersistedSession actual = codec.decode(codec.encode(expected));

        assertEquals(
                "Refresh token must survive encoding.",
                expected.refreshToken(),
                actual.refreshToken());
        assertSessionMetadata(expected, actual);
    }

    @Test(expected = SessionStorageException.class)
    public void rejectsUnknownOrCorruptFormat() {
        new SessionCodec().decode(new byte[] {0, 1, 2, 3, 4, 5, 6, 7});
    }

    private static void assertSessionMetadata(
            PersistedSession expected, PersistedSession actual) {
        assertEquals(
                "Refresh expiry must survive encoding.",
                expected.refreshTokenExpiresAt(),
                actual.refreshTokenExpiresAt());
        assertEquals(
                "Absolute expiry must survive encoding.",
                expected.sessionAbsoluteExpiresAt(),
                actual.sessionAbsoluteExpiresAt());
        assertEquals(
                "Session ID must survive encoding.",
                expected.sessionId(),
                actual.sessionId());
        assertEquals(
                "Device ID must survive encoding.",
                expected.deviceId(),
                actual.deviceId());
    }
}
