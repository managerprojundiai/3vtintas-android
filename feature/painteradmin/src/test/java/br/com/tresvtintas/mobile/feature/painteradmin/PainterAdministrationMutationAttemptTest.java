package br.com.tresvtintas.mobile.feature.painteradmin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class PainterAdministrationMutationAttemptTest {
    private static final String PAYLOAD =
            "commission\n12\n4\n3.50";

    @Test
    public void reusesKeyOnlyForTheSameCanonicalMutation() {
        PainterAdministrationMutationAttempt attempt =
                new PainterAdministrationMutationAttempt();

        String first = attempt.keyFor(PAYLOAD);
        String retry = attempt.keyFor(PAYLOAD);
        String changed = attempt.keyFor(
                "commission\n12\n4\n4.00");

        assertEquals(
                "A safe retry must reuse its idempotency key.",
                first,
                retry);
        assertNotEquals(
                "A changed payload must create a new logical mutation.",
                first,
                changed);
    }

    @Test
    public void restoresOnlyOpaqueKeyAndDigest() {
        PainterAdministrationMutationAttempt attempt =
                new PainterAdministrationMutationAttempt();
        String key = attempt.keyFor(PAYLOAD);

        PainterAdministrationMutationAttempt restored =
                PainterAdministrationMutationAttempt.restored(
                        attempt.key(),
                        attempt.fingerprint());

        assertEquals(
                "Configuration recreation must preserve a safe retry.",
                key,
                restored.keyFor(PAYLOAD));
        assertEquals(
                "Only the fixed-size SHA-256 digest may be retained.",
                64,
                restored.fingerprint().length());
        assertTrue(
                "Persisted state must not contain the mutation payload.",
                !restored.fingerprint().contains("commission")
                        && !restored.key().contains("commission"));
    }

    @Test
    public void malformedStateAndEmptyPayloadFailClosed() {
        PainterAdministrationMutationAttempt restored =
                PainterAdministrationMutationAttempt.restored(
                        "short",
                        PAYLOAD);

        assertNotEquals(
                "Malformed state must never be trusted.",
                "short",
                restored.keyFor(PAYLOAD));
        assertThrows(
                IllegalArgumentException.class,
                () -> restored.keyFor(""));
    }

    @Test
    public void resetStartsANewLogicalMutation() {
        PainterAdministrationMutationAttempt attempt =
                new PainterAdministrationMutationAttempt();
        String first = attempt.keyFor(PAYLOAD);

        attempt.reset();

        assertNotEquals(
                "Terminal success must create a fresh future attempt.",
                first,
                attempt.keyFor(PAYLOAD));
    }
}
