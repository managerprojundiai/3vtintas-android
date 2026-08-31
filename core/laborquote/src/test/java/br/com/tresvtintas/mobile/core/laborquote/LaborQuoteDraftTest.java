package br.com.tresvtintas.mobile.core.laborquote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.Test;

public final class LaborQuoteDraftTest {
    @Test
    public void calculatesCanonicalTotals() {
        LaborQuoteDraft draft = new LaborQuoteDraft(
                7,
                "Cliente",
                Optional.of(" Pintura "),
                Optional.empty(),
                Optional.empty(),
                new BigDecimal("25"),
                List.of(new LaborQuoteDraftLine(
                        "Parede",
                        BigDecimal.TEN,
                        "m²",
                        new BigDecimal("30"))));

        assertEquals("Subtotal must be canonical.",
                new BigDecimal("300.00"), draft.subtotal());
        assertEquals("Discount must reduce the total.",
                new BigDecimal("275.00"), draft.total());
        assertEquals("Text must be normalized.",
                "Pintura", draft.title().orElseThrow());
    }

    @Test
    public void refusesDiscountAboveSubtotal() {
        assertThrows(IllegalArgumentException.class, () -> new LaborQuoteDraft(
                7,
                "Cliente",
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                new BigDecimal("301"),
                List.of(new LaborQuoteDraftLine(
                        "Parede",
                        BigDecimal.TEN,
                        "m²",
                        new BigDecimal("30")))));
    }
}
