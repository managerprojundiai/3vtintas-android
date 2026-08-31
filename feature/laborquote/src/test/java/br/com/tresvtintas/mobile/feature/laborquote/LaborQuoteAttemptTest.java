package br.com.tresvtintas.mobile.feature.laborquote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteDraft;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteDraftLine;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.Test;

public final class LaborQuoteAttemptTest {
    @Test
    public void reusesKeyOnlyForIdenticalDraftAndRevision() {
        LaborQuoteMutationAttempt attempt = new LaborQuoteMutationAttempt();
        String first = attempt.keyFor(draft("25"), 1);

        assertEquals("Identical retry must preserve its key.",
                first, attempt.keyFor(draft("25"), 1));
        assertNotEquals("Changed price must rotate the key.",
                first, attempt.keyFor(draft("26"), 1));
        assertNotEquals("Changed revision must rotate the key.",
                attempt.keyFor(draft("26"), 1), attempt.keyFor(draft("26"), 2));
    }

    @Test
    public void statusAttemptChangesWithRevisionOrTarget() {
        LaborQuoteStatusAttempt attempt = new LaborQuoteStatusAttempt();
        String first = attempt.keyFor(LaborQuoteStatus.SENT, 1);

        assertEquals("Identical status retry must preserve its key.",
                first, attempt.keyFor(LaborQuoteStatus.SENT, 1));
        assertNotEquals("Changed status must rotate the key.",
                first, attempt.keyFor(LaborQuoteStatus.ACCEPTED, 1));
        assertNotEquals(
                "Changed revision must rotate the status key.",
                attempt.keyFor(LaborQuoteStatus.ACCEPTED, 1),
                attempt.keyFor(LaborQuoteStatus.ACCEPTED, 2));
    }

    private static LaborQuoteDraft draft(String price) {
        return new LaborQuoteDraft(
                1,
                "Cliente",
                Optional.of("Pintura"),
                Optional.empty(),
                Optional.empty(),
                BigDecimal.ZERO,
                List.of(new LaborQuoteDraftLine(
                        "Serviço",
                        BigDecimal.ONE,
                        "un",
                        new BigDecimal(price))));
    }
}
