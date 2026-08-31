package br.com.tresvtintas.mobile.core.delivery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import java.time.Instant;
import java.util.Optional;
import org.junit.Test;

public final class DeliveryQueryTest {
    @Test
    public void normalizesSearchAndClearsStatusWhenViewChanges() {
        DeliveryQuery query = DeliveryQuery.initial()
                .withSearch("  Pedido 701  ");

        assertEquals(
                "Search must be normalized once at the boundary.",
                Optional.of("Pedido 701"),
                query.search());
        assertEquals(
                "The initial operational view must be active.",
                DeliveryView.ACTIVE,
                query.view());
        assertEquals(
                "Changing view must preserve the normalized search.",
                Optional.of("Pedido 701"),
                query.withView(DeliveryView.HISTORY).search());
    }

    @Test
    public void rejectsOversizedSearch() {
        assertThrows(
                IllegalArgumentException.class,
                () -> assertNotNull(
                        "The invalid result must never be accepted.",
                        DeliveryQuery.initial().withSearch(
                                "x".repeat(81))));
    }

    @Test
    public void createsOnlyACompleteCalendarWindow() {
        Instant from = Instant.parse("2026-07-26T03:00:00Z");
        Instant to = Instant.parse("2026-09-06T03:00:00Z");

        DeliveryQuery calendar = DeliveryQuery.initial()
                .forAgendaWindow(from, to);

        assertEquals(
                "Agenda projection must use the dedicated logistics view.",
                DeliveryView.CALENDAR,
                calendar.view());
        assertEquals(
                "Agenda projection must preserve the exclusive upper bound.",
                Optional.of(to),
                calendar.scheduledToExclusive());
        assertThrows(
                IllegalArgumentException.class,
                () -> assertNotNull(
                        "An inverted calendar window must never be accepted.",
                        DeliveryQuery.initial().forAgendaWindow(to, from)));
    }
}
