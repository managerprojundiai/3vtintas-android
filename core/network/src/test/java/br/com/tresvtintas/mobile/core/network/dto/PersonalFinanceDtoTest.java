package br.com.tresvtintas.mobile.core.network.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.List;
import org.junit.Test;

public final class PersonalFinanceDtoTest {
    @Test
    public void acceptsStrictOwnedSummaryAndExactMoney() {
        PersonalFinanceSummaryDto value = summary(
                "manual",
                List.of("settle", "cancel"));

        assertEquals(
                "Money must remain an exact decimal string.",
                "250.00",
                value.amount());
        assertEquals(
                "Only the two explicit manual actions are accepted.",
                2,
                value.allowedActions().size());
    }

    @Test
    public void rejectsActionsOnUnknownSourceAndDuplicateActions() {
        assertThrows(
                IllegalArgumentException.class,
                () -> summary("legacy", List.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> summary("manual", List.of("settle", "settle")));
    }

    @Test
    public void requiresLiteralMutationConfirmation() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new PersonalFinanceCreateRequest(
                        "expense",
                        "Combustível",
                        "50.00",
                        null,
                        null,
                        null,
                        "yes"));
        new PersonalFinanceCancellationRequest(
                "CANCEL_PERSONAL_FINANCIAL_ENTRY");
    }

    private static PersonalFinanceSummaryDto summary(
            String source,
            List<String> actions) {
        return new PersonalFinanceSummaryDto(
                701,
                "receivable",
                "pending",
                source,
                "Cliente João",
                "250.00",
                "BRL",
                "2026-07-30T03:00:00Z",
                null,
                new PersonalFinanceSummaryDto.Customer(91, "João"),
                actions,
                "2026-07-26T12:00:00Z",
                "2026-07-26T12:05:00Z");
    }
}
