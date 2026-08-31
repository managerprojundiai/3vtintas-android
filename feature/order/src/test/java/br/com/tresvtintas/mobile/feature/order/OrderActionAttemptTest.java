package br.com.tresvtintas.mobile.feature.order;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import br.com.tresvtintas.mobile.core.order.OrderAction;
import org.junit.Test;

public final class OrderActionAttemptTest {
    @Test
    public void preservesKeyForSameLogicalRetryAndReplacesItForNewPayload() {
        OrderActionAttempt attempt = new OrderActionAttempt();

        String first = attempt.keyFor(
                OrderAction.RECORD_PAYMENT,
                701,
                4,
                "PIX\ncomprovante");
        String retry = attempt.keyFor(
                OrderAction.RECORD_PAYMENT,
                701,
                4,
                "PIX\ncomprovante");
        String changed = attempt.keyFor(
                OrderAction.RECORD_PAYMENT,
                701,
                4,
                "PIX\noutro");

        assertEquals(
                "A retry of the same logical action must reuse its key.",
                first,
                retry);
        assertNotEquals(
                "A changed payload must receive another key.",
                first,
                changed);
    }

    @Test
    public void rejectsInvalidRestoredKey() {
        OrderActionAttempt attempt = OrderActionAttempt.restored(
                "invalid",
                "stale");

        String generated = attempt.keyFor(
                OrderAction.CANCEL,
                701,
                2,
                "");

        assertNotEquals(
                "An invalid restored key must never be reused.",
                "invalid",
                generated);
    }
}
