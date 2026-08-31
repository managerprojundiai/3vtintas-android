package br.com.tresvtintas.mobile.feature.laborquote;

import static org.junit.Assert.assertEquals;

import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteDraftLine;
import java.math.BigDecimal;
import org.junit.Test;

public final class LaborQuoteEditorModelTest {
    @Test
    public void editsIndependentManualServiceLines() {
        LaborQuoteEditorModel model = new LaborQuoteEditorModel();
        model.add(line("Preparação", "10", "12.50"));
        model.add(line("Pintura", "10", "25"));
        model.replace(0, line("Preparação fina", "10", "15"));

        assertEquals("Editor subtotal must follow all service lines.",
                new BigDecimal("400.00"), model.subtotal());
        assertEquals("Editor discount must reduce the estimate.",
                new BigDecimal("350.00"), model.total(new BigDecimal("50")));
        assertEquals("Edited service must replace its position.",
                "Preparação fina", model.lines().get(0).description());
    }

    private static LaborQuoteDraftLine line(String name, String quantity, String price) {
        return new LaborQuoteDraftLine(
                name,
                new BigDecimal(quantity),
                "m²",
                new BigDecimal(price));
    }
}
