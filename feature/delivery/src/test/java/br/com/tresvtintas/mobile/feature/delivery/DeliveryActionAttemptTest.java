package br.com.tresvtintas.mobile.feature.delivery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import br.com.tresvtintas.mobile.core.delivery.DeliveryAction;
import org.junit.Test;

public final class DeliveryActionAttemptTest {
    @Test
    public void preservesKeyForRetryAndChangesItForNewRevision() {
        DeliveryActionAttempt attempt = new DeliveryActionAttempt();

        String first = attempt.keyFor(
                DeliveryAction.START,
                801,
                4);
        String retry = attempt.keyFor(
                DeliveryAction.START,
                801,
                4);
        String changed = attempt.keyFor(
                DeliveryAction.START,
                801,
                5);

        assertEquals(
                "A logical retry must reuse the same key.",
                first,
                retry);
        assertNotEquals(
                "A new revision must receive a new key.",
                first,
                changed);
    }

    @Test
    public void rejectsInvalidRestoredKey() {
        DeliveryActionAttempt attempt =
                DeliveryActionAttempt.restored(
                        "invalid",
                        "stale");

        String generated = attempt.keyFor(
                DeliveryAction.COMPLETE,
                801,
                5);

        assertNotEquals(
                "An invalid restored key must not be reused.",
                "invalid",
                generated);
    }
}
