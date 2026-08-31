package br.com.tresvtintas.mobile.core.quote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.Optional;
import org.junit.Test;

public final class MaterialQuoteQueryTest {
    @Test
    public void defaultsTheLandingWorklistToActive() {
        assertEquals(
                "Pending work must be the landing view.",
                MaterialQuoteView.ACTIVE,
                MaterialQuoteQuery.initial().view());
    }

    @Test
    public void rejectsAmbiguousStatusAndLifecycleFilters() {
        assertThrows(
                "Status and lifecycle view cannot be ambiguous.",
                IllegalArgumentException.class,
                () -> new MaterialQuoteQuery(
                        Optional.empty(),
                        Optional.of(MaterialQuoteStatus.DRAFT),
                        MaterialQuoteView.HISTORY,
                        30));
    }
}
