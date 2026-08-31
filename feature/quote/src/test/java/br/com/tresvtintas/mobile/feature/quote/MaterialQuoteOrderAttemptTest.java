package br.com.tresvtintas.mobile.feature.quote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public final class MaterialQuoteOrderAttemptTest {
    @Test
    public void reusesKeyOnlyForSameQuoteRevision() {
        MaterialQuoteOrderAttempt attempt = new MaterialQuoteOrderAttempt();
        String first = attempt.keyFor(10, 3);
        assertEquals(first, attempt.keyFor(10, 3));
        assertNotEquals(first, attempt.keyFor(10, 4));
    }

    @Test
    public void discardsInvalidRestoredState() {
        MaterialQuoteOrderAttempt attempt = MaterialQuoteOrderAttempt.restored("invalid", 10, 3);
        assertEquals("", attempt.key());
        assertEquals(0, attempt.quoteId());
    }
}
