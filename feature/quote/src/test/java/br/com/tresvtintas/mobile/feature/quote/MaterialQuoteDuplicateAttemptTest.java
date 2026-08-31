package br.com.tresvtintas.mobile.feature.quote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public final class MaterialQuoteDuplicateAttemptTest {
    @Test
    public void reusesTheKeyForTheSameSourceUntilSuccess() {
        MaterialQuoteDuplicateAttempt attempt = new MaterialQuoteDuplicateAttempt();
        String first = attempt.keyFor(501, 4);
        assertEquals("Retry must reuse the same key.",
                first, attempt.keyFor(501, 4));

        attempt.reset();
        assertNotEquals("Success must release the old key.",
                first, attempt.keyFor(501, 4));
    }

    @Test
    public void restoresOnlyAValidBoundKey() {
        MaterialQuoteDuplicateAttempt source = new MaterialQuoteDuplicateAttempt();
        String key = source.keyFor(501, 4);
        assertEquals(
                "Rotation must preserve the bound key.",
                key,
                MaterialQuoteDuplicateAttempt.restored(key, 501, 4)
                        .keyFor(501, 4));
        assertNotEquals(
                "Invalid restored keys must be discarded.",
                "invalid",
                MaterialQuoteDuplicateAttempt.restored("invalid", 501, 4)
                        .keyFor(501, 4));
    }
}
