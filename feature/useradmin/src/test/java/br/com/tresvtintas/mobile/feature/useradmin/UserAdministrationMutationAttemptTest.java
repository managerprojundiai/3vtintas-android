package br.com.tresvtintas.mobile.feature.useradmin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class UserAdministrationMutationAttemptTest {
    private static final String PAYLOAD = "operational_role\n31\n4\nsalesperson\n7";

    @Test
    public void reusesKeyOnlyForSameCanonicalMutation() {
        UserAdministrationMutationAttempt attempt =
                new UserAdministrationMutationAttempt();

        String first = attempt.keyFor(PAYLOAD);
        String retry = attempt.keyFor(PAYLOAD);
        String changed = attempt.keyFor(
                "operational_role\n31\n4\nsalesperson\n8");

        assertEquals(
                "A safe retry must reuse its idempotency key.",
                first,
                retry);
        assertNotEquals(
                "A changed binding must create a new mutation key.",
                first,
                changed);
    }

    @Test
    public void persistsOnlyOpaqueKeyAndDigest() {
        UserAdministrationMutationAttempt attempt =
                new UserAdministrationMutationAttempt();
        String key = attempt.keyFor(PAYLOAD);
        UserAdministrationMutationAttempt restored =
                UserAdministrationMutationAttempt.restored(
                        attempt.key(),
                        attempt.fingerprint());

        assertEquals(
                "Configuration recreation must preserve safe retry state.",
                key,
                restored.keyFor(PAYLOAD));
        assertEquals(
                "Only a SHA-256 digest may be retained.",
                64,
                restored.fingerprint().length());
        assertTrue(
                "Persisted state must not expose the mutation payload.",
                !restored.fingerprint().contains("salesperson")
                        && !restored.key().contains("salesperson"));
    }

    @Test
    public void malformedStateAndEmptyPayloadFailClosed() {
        UserAdministrationMutationAttempt restored =
                UserAdministrationMutationAttempt.restored(
                        "short",
                        PAYLOAD);

        assertNotEquals(
                "Malformed persisted state must never be trusted.",
                "short",
                restored.keyFor(PAYLOAD));
        assertThrows(
                IllegalArgumentException.class,
                () -> restored.keyFor(""));
    }

    @Test
    public void terminalResetStartsNewLogicalMutation() {
        UserAdministrationMutationAttempt attempt =
                new UserAdministrationMutationAttempt();
        String first = attempt.keyFor(PAYLOAD);

        attempt.reset();

        assertNotEquals(
                "A terminal result must create a fresh future attempt.",
                first,
                attempt.keyFor(PAYLOAD));
    }
}
