package br.com.tresvtintas.mobile.core.finance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.Test;

public final class FinanceQueryTest {
    @Test
    public void normalizesSearchAndPreservesExplicitFilters() {
        FinanceQuery query = new FinanceQuery(
                Optional.of("  Cliente JOÃO  "),
                Optional.of(FinanceEntryStatus.PENDING),
                Optional.of(FinanceEntryType.RECEIVABLE),
                Optional.of(FinanceEntrySource.MANUAL),
                FinanceDueFilter.UPCOMING,
                FinanceDateBasis.DUE,
                Optional.of(Instant.parse("2026-07-26T03:00:00Z")),
                Optional.of(Instant.parse("2026-08-01T03:00:00Z")),
                25);

        assertEquals(
                "Search must be normalized before leaving the domain.",
                Optional.of("cliente joão"),
                query.search());
        assertEquals(
                "Due filter must remain explicit.",
                FinanceDueFilter.UPCOMING,
                query.due());
        assertEquals(
                "Source filter must remain explicit.",
                Optional.of(FinanceEntrySource.MANUAL),
                query.source());
        assertEquals("Page size must remain bounded.", 25, query.pageSize());
    }

    @Test
    public void rejectsInvalidPeriodsAndConflictingDueFilters() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new FinanceQuery(
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        FinanceDueFilter.WITHOUT_DUE_DATE,
                        FinanceDateBasis.DUE,
                        Optional.of(Instant.parse("2026-07-01T00:00:00Z")),
                        Optional.empty(),
                        30));
        assertThrows(
                IllegalArgumentException.class,
                () -> new FinanceQuery(
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        FinanceDueFilter.ALL,
                        FinanceDateBasis.DUE,
                        Optional.of(Instant.parse("2026-08-01T00:00:00Z")),
                        Optional.of(Instant.parse("2026-07-01T00:00:00Z")),
                        30));
    }

    @Test
    public void agendaWindowUsesEffectiveDateWithoutChangingFinanceDefaults() {
        FinanceQuery query = FinanceQuery.initial().forAgendaWindow(
                Instant.parse("2026-07-01T03:00:00Z"),
                Instant.parse("2026-08-01T03:00:00Z"));

        assertEquals(
                "Agenda projection must request the effective finance date.",
                FinanceDateBasis.AGENDA,
                query.dateBasis());
        assertEquals(
                "Agenda projection must not combine due-state semantics.",
                FinanceDueFilter.ALL,
                query.due());
        assertEquals(
                "Regular finance queries must remain due based.",
                FinanceDateBasis.DUE,
                FinanceQuery.initial().dateBasis());
    }

    @Test
    public void draftRequiresPositiveExactMoneyAndValidOptionalCustomer() {
        FinanceDraft draft = new FinanceDraft(
                FinanceEntryType.EXPENSE,
                "  Combustível  ",
                new BigDecimal("25.5"),
                Optional.empty(),
                OptionalLong.of(91),
                Optional.of("  visita externa  "));

        assertEquals("Title must be trimmed.", "Combustível", draft.title());
        assertEquals(
                "Money must be normalized to two decimals.",
                "25.50",
                draft.amount().toPlainString());
        assertEquals(
                "Notes must be normalized.",
                Optional.of("visita externa"),
                draft.notes());
        assertThrows(
                IllegalArgumentException.class,
                () -> new FinanceDraft(
                        FinanceEntryType.EXPENSE,
                        "Inválido",
                        new BigDecimal("-1.00"),
                        Optional.empty(),
                        OptionalLong.empty(),
                        Optional.empty()));
    }
}
