package br.com.tresvtintas.mobile.core.accountaccess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.Test;

public final class AccountAccessModelsTest {
    private static final String DEVICE_ID =
            "10000000-0000-4000-8000-000000000001";
    private static final String OTHER_DEVICE_ID =
            "10000000-0000-4000-8000-000000000002";
    private static final String SESSION_ID =
            "20000000-0000-4000-8000-000000000001";
    private static final Instant ISSUED =
            Instant.parse("2026-07-27T08:00:00Z");
    private static final Instant LAST_SEEN =
            Instant.parse("2026-07-27T09:00:00Z");

    @Test
    public void expiredSessionMayBeDerivedWithoutEndTimestamp() {
        AccountSession session = session(
                AccountAccessStatus.EXPIRED,
                Optional.empty(),
                false);

        assertEquals(
                "Server-derived expiry must remain distinguishable from revocation.",
                AccountAccessStatus.EXPIRED,
                session.status());
        assertTrue(
                "Natural expiry does not require a revocation timestamp.",
                session.endedAt().isEmpty());
    }

    @Test
    public void currentAccessMustRemainActive() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AccountDevice(
                        DEVICE_ID,
                        "Tablet",
                        Optional.of("Samsung"),
                        Optional.of("SM-X"),
                        35,
                        "0.30.0",
                        AccountAccessStatus.REVOKED,
                        ISSUED,
                        LAST_SEEN,
                        Optional.of(LAST_SEEN),
                        true));
        assertThrows(
                IllegalArgumentException.class,
                () -> session(
                        AccountAccessStatus.EXPIRED,
                        Optional.empty(),
                        true));
    }

    @Test
    public void timestampsFailClosedWhenTheirOrderIsImpossible() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AccountDevice(
                        DEVICE_ID,
                        "Tablet",
                        Optional.empty(),
                        Optional.empty(),
                        35,
                        "0.30.0",
                        AccountAccessStatus.ACTIVE,
                        LAST_SEEN,
                        ISSUED,
                        Optional.empty(),
                        false));
        assertThrows(
                IllegalArgumentException.class,
                () -> new AccountSession(
                        SESSION_ID,
                        DEVICE_ID,
                        AccountAuthMethod.GOOGLE,
                        AccountAccessStatus.ACTIVE,
                        LAST_SEEN,
                        ISSUED,
                        Instant.parse("2026-07-27T10:00:00Z"),
                        Instant.parse("2026-08-27T08:00:00Z"),
                        Optional.empty(),
                        false));
    }

    @Test
    public void pageRejectsDuplicatesAndSnapshotRejectsMixedViews() {
        AccountDevice device = device(DEVICE_ID, true);

        assertThrows(
                IllegalArgumentException.class,
                () -> new AccountAccessPage(
                        List.of(device, device),
                        Optional.empty()));
        AccountAccessPage sessionPage = new AccountAccessPage(
                List.of(session(
                        AccountAccessStatus.ACTIVE,
                        Optional.empty(),
                        true)),
                Optional.empty());
        assertThrows(
                IllegalArgumentException.class,
                () -> AccountAccessSnapshot.from(
                        AccountAccessView.DEVICES,
                        sessionPage));
    }

    @Test
    public void appendDeduplicatesCursorOverlapAndPreservesNewestEntry() {
        AccountDevice first = device(DEVICE_ID, true);
        AccountDevice second = device(OTHER_DEVICE_ID, false);
        AccountAccessSnapshot snapshot = AccountAccessSnapshot.from(
                AccountAccessView.DEVICES,
                new AccountAccessPage(
                        List.of(first),
                        Optional.of("next")));

        AccountAccessSnapshot appended = snapshot.append(
                new AccountAccessPage(
                        List.of(first, second),
                        Optional.empty()));

        assertEquals(
                "Cursor overlap must not duplicate a device.",
                2,
                appended.items().size());
        assertFalse(
                "The final page must clear the next cursor.",
                appended.hasMore());
    }

    static AccountDevice device(String id, boolean current) {
        return new AccountDevice(
                id,
                current ? "Tablet atual" : "Celular antigo",
                Optional.of("Samsung"),
                Optional.of("Modelo"),
                35,
                "0.30.0",
                AccountAccessStatus.ACTIVE,
                ISSUED,
                LAST_SEEN,
                Optional.empty(),
                current);
    }

    static AccountSession session(
            AccountAccessStatus status,
            Optional<Instant> endedAt,
            boolean current) {
        return new AccountSession(
                SESSION_ID,
                DEVICE_ID,
                AccountAuthMethod.GOOGLE,
                status,
                ISSUED,
                LAST_SEEN,
                Instant.parse("2026-07-27T10:00:00Z"),
                Instant.parse("2026-08-27T08:00:00Z"),
                endedAt,
                current);
    }
}
