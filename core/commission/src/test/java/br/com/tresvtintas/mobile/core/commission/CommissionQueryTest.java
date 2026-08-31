package br.com.tresvtintas.mobile.core.commission;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.time.LocalDate;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.Test;

public final class CommissionQueryTest {
    @Test
    public void normalizesSearchAndRetainsAuthoritativeFilters() {
        CommissionQuery query = new CommissionQuery(
                CommissionScope.TEAM,
                Optional.of("  MARIA  "),
                Optional.empty(),
                Optional.of(CommissionKind.SELLER),
                OptionalLong.of(9),
                Optional.of(LocalDate.parse("2026-07-01")),
                Optional.of(LocalDate.parse("2026-07-26")),
                30).withStatus(CommissionStatus.APPROVED);

        assertEquals(
                "Search must be normalized before reaching the repository.",
                Optional.of("maria"),
                query.search());
        assertEquals(
                "Changing status must preserve the requested access scope.",
                CommissionScope.TEAM,
                query.scope());
        assertEquals(
                "Changing status must preserve the server-validated organization filter.",
                OptionalLong.of(9),
                query.organizationId());
        assertEquals(
                "Changing status must preserve the commission kind.",
                Optional.of(CommissionKind.SELLER),
                query.kind());
        assertEquals(
                "The selected status must replace the prior status.",
                Optional.of(CommissionStatus.APPROVED),
                query.status());
    }

    @Test
    public void rejectsUnpairedReversedAndOversizedPeriods() {
        assertThrows(
                "Commission dates must always be supplied as a pair.",
                IllegalArgumentException.class,
                () -> query(
                        Optional.of(LocalDate.parse("2026-07-01")),
                        Optional.empty()));
        assertThrows(
                "The end date cannot precede the start date.",
                IllegalArgumentException.class,
                () -> query(
                        Optional.of(LocalDate.parse("2026-07-26")),
                        Optional.of(LocalDate.parse("2026-07-01"))));
        assertThrows(
                "A mobile commission period cannot exceed 366 days.",
                IllegalArgumentException.class,
                () -> query(
                        Optional.of(LocalDate.parse("2025-01-01")),
                        Optional.of(LocalDate.parse("2026-01-03"))));
    }

    @Test
    public void rejectsUnboundedOrAmbiguousFilters() {
        assertThrows(
                "Search input must remain bounded.",
                IllegalArgumentException.class,
                () -> new CommissionQuery(
                        CommissionScope.SELF,
                        Optional.of("x".repeat(81)),
                        Optional.empty(),
                        Optional.empty(),
                        OptionalLong.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        30));
        assertThrows(
                "Organization IDs must be positive.",
                IllegalArgumentException.class,
                () -> new CommissionQuery(
                        CommissionScope.TEAM,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        OptionalLong.of(0),
                        Optional.empty(),
                        Optional.empty(),
                        30));
        assertThrows(
                "Page size must stay inside the public contract.",
                IllegalArgumentException.class,
                () -> new CommissionQuery(
                        CommissionScope.SELF,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        OptionalLong.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        101));
    }

    private static CommissionQuery query(
            Optional<LocalDate> from,
            Optional<LocalDate> to) {
        return new CommissionQuery(
                CommissionScope.SELF,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                OptionalLong.empty(),
                from,
                to,
                30);
    }
}
