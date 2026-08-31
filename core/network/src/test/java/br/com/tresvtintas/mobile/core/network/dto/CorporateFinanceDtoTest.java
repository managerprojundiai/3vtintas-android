package br.com.tresvtintas.mobile.core.network.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.List;
import org.junit.Test;

public final class CorporateFinanceDtoTest {
    @Test
    public void acceptsProtectedOrderSourceAndOrganizationContext() {
        CorporateFinanceSummaryDto value = summary(
                "material_order",
                List.of());

        assertEquals(
                "Money must remain an exact decimal string.",
                "123.45",
                value.amount());
        assertEquals(
                "Organization ID must remain explicit.",
                7,
                value.organization().id());
        assertEquals(
                "Organization name must remain explicit.",
                "Loja Centro",
                value.organization().name());
    }

    @Test
    public void rejectsUnknownSourceAndDuplicateActions() {
        assertThrows(
                IllegalArgumentException.class,
                () -> summary("legacy", List.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> summary(
                        "manual",
                        List.of("settle", "settle")));
    }

    @Test
    public void requiresOrganizationAndLiteralConfirmations() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new CorporateFinanceCreateRequest(
                        0,
                        "expense",
                        "Combustível",
                        "50.00",
                        null,
                        null,
                        null,
                        "CREATE_CORPORATE_FINANCIAL_ENTRY"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new CorporateFinanceCancellationRequest("yes"));
        new CorporateFinanceCancellationRequest(
                "CANCEL_CORPORATE_FINANCIAL_ENTRY");
    }

    private static CorporateFinanceSummaryDto summary(
            String source,
            List<String> actions) {
        return new CorporateFinanceSummaryDto(
                701,
                "payable",
                "pending",
                source,
                "Pedido 3001",
                "123.45",
                "BRL",
                "2026-07-30T03:00:00Z",
                null,
                new CorporateFinanceOrganizationDto(
                        7,
                        "Loja Centro"),
                null,
                actions,
                "2026-07-26T12:00:00Z",
                "2026-07-26T12:05:00Z");
    }
}
