package br.com.tresvtintas.mobile.core.laborquote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.Optional;
import org.junit.Test;

public final class LaborQuoteQueryTest {
    @Test
    public void defaultsTheLandingWorklistToActive() {
        assertEquals(
                "Pending work must be the landing view.",
                LaborQuoteView.ACTIVE,
                LaborQuoteQuery.initial().view());
    }

    @Test
    public void rejectsAmbiguousStatusAndLifecycleFilters() {
        assertThrows(
                "Status and lifecycle view cannot be ambiguous.",
                IllegalArgumentException.class,
                () -> new LaborQuoteQuery(
                        Optional.empty(),
                        Optional.of(LaborQuoteStatus.SENT),
                        LaborQuoteView.HISTORY,
                        30));
    }
}
