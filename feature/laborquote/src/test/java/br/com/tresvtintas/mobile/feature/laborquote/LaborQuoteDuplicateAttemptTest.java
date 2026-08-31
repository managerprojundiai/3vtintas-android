package br.com.tresvtintas.mobile.feature.laborquote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public final class LaborQuoteDuplicateAttemptTest {
    @Test
    public void keepsTheIdempotencyKeyAcrossRotationAndRetry() {
        LaborQuoteDuplicateAttempt attempt = new LaborQuoteDuplicateAttempt();
        String key = attempt.keyFor(701, 3);
        LaborQuoteDuplicateAttempt restored =
                LaborQuoteDuplicateAttempt.restored(key, 701, 3);

        assertEquals("Rotation must preserve the same operation key.",
                key, restored.keyFor(701, 3));
        assertNotEquals("A changed source revision needs a new key.",
                key, restored.keyFor(701, 4));
    }
}
