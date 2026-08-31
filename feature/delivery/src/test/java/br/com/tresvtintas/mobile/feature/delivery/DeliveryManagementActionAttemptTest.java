package br.com.tresvtintas.mobile.feature.delivery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import br.com.tresvtintas.mobile.core.delivery.DeliveryManagementAction;
import org.junit.Test;

public final class DeliveryManagementActionAttemptTest {
    @Test
    public void preservesKeyOnlyForTheSameLogicalCommand() {
        DeliveryManagementActionAttempt attempt =
                new DeliveryManagementActionAttempt();

        String first = attempt.keyFor(
                DeliveryManagementAction.ASSIGN,
                7,
                501,
                3,
                "21");
        String retry = attempt.keyFor(
                DeliveryManagementAction.ASSIGN,
                7,
                501,
                3,
                "21");
        String otherDriver = attempt.keyFor(
                DeliveryManagementAction.ASSIGN,
                7,
                501,
                3,
                "22");

        assertEquals(
                "A logical retry must reuse the same idempotency key.",
                first,
                retry);
        assertNotEquals(
                "A different driver must receive another key.",
                first,
                otherDriver);
    }

    @Test
    public void restoredAttemptRetainsStableKey() {
        String key = "00000000-0000-4000-8000-000000000903";
        String fingerprint = "SCHEDULE|7|501|3|2026-07-27T12:00:00Z";
        DeliveryManagementActionAttempt attempt =
                DeliveryManagementActionAttempt.restored(key, fingerprint);

        assertEquals(
                "A restored logical attempt must retain its key.",
                key,
                attempt.keyFor(
                        DeliveryManagementAction.SCHEDULE,
                        7,
                        501,
                        3,
                        "2026-07-27T12:00:00Z"));
    }
}
