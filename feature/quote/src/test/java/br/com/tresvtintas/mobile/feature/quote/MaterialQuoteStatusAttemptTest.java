package br.com.tresvtintas.mobile.feature.quote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import br.com.tresvtintas.mobile.core.quote.MaterialQuoteStatus;
import org.junit.Test;

public final class MaterialQuoteStatusAttemptTest {
    @Test
    public void reusesKeyOnlyForSameTargetAndRevision() {
        MaterialQuoteStatusAttempt attempt = new MaterialQuoteStatusAttempt();
        String first = attempt.keyFor(MaterialQuoteStatus.SENT, 3);

        assertEquals("Identical attempt must reuse its key.",
                first, attempt.keyFor(MaterialQuoteStatus.SENT, 3));
        assertNotEquals("A new status must rotate the key.",
                first, attempt.keyFor(MaterialQuoteStatus.ACCEPTED, 3));
        assertNotEquals(
                "A new revision must rotate the key.",
                attempt.keyFor(MaterialQuoteStatus.ACCEPTED, 3),
                attempt.keyFor(MaterialQuoteStatus.ACCEPTED, 4));
    }
}
