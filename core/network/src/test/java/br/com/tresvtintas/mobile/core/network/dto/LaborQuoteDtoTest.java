package br.com.tresvtintas.mobile.core.network.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public final class LaborQuoteDtoTest {
    @Test
    public void laborQuotePageDefensivelyCopiesItems() {
        List<LaborQuoteSummaryDto> mutable = new ArrayList<>();
        mutable.add(summary());

        LaborQuotePageDto page = new LaborQuotePageDto(mutable, null);
        mutable.clear();

        assertEquals("DTO must preserve its own immutable list.", 1, page.items().size());
        assertThrows(
                UnsupportedOperationException.class,
                () -> page.items().add(summary()));
    }

    @Test
    public void laborQuoteRequestRejectsInvalidMoneyAndStatus() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new LaborQuoteLineRequest("Pintura", "1.000", "m2", "50.00"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new LaborQuoteStatusMutationRequest(1, "converted"));
    }

    private static LaborQuoteSummaryDto summary() {
        LaborQuotePersonDto customer = new LaborQuotePersonDto(10, "Cliente");
        LaborQuotePersonDto painter = new LaborQuotePersonDto(20, "Pintor");
        return new LaborQuoteSummaryDto(
                30,
                customer,
                painter,
                40L,
                "Pintura residencial",
                "draft",
                "100.00",
                "0.00",
                "100.00",
                1,
                1,
                null,
                "2026-07-25T12:00:00Z",
                "2026-07-25T12:00:00Z");
    }
}
