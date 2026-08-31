package br.com.tresvtintas.mobile.core.appointment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.delivery.DeliveryAction;
import br.com.tresvtintas.mobile.core.delivery.DeliveryStatus;
import br.com.tresvtintas.mobile.core.delivery.DeliverySummary;
import br.com.tresvtintas.mobile.core.finance.FinanceAction;
import br.com.tresvtintas.mobile.core.finance.FinanceEntrySource;
import br.com.tresvtintas.mobile.core.finance.FinanceEntryStatus;
import br.com.tresvtintas.mobile.core.finance.FinanceEntryType;
import br.com.tresvtintas.mobile.core.finance.FinanceSummary;
import br.com.tresvtintas.mobile.core.model.AppRole;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;

public final class AgendaMonthSnapshotTest {
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final Instant REFERENCE =
            Instant.parse("2026-07-29T12:00:00Z");

    @Test
    public void buildsFortyTwoDayGridAndKeepsSourcesDistinct() {
        AgendaMonthSnapshot snapshot = AgendaMonthSnapshot.from(
                YearMonth.of(2026, 8),
                LocalDate.of(2026, 8, 4),
                ZONE,
                List.of(
                        appointment(
                                1,
                                AppointmentKind.GENERAL,
                                "2026-08-04T13:00:00Z"),
                        appointment(
                                2,
                                AppointmentKind.DELIVERY,
                                "2026-08-04T15:00:00Z")),
                List.of(),
                List.of(
                        finance(
                                3,
                                FinanceEntryType.RECEIVABLE,
                                "125.40",
                                "2026-08-04T16:00:00Z"),
                        finance(
                                4,
                                FinanceEntryType.PAYABLE,
                                "30.00",
                                "2026-08-04T17:00:00Z")));

        AgendaDay day = snapshot.selectedDay();
        assertEquals(
                "A professional month grid must always expose six weeks.",
                42,
                snapshot.visibleDays().size());
        assertEquals(
                "All authorized sources for the selected day must be visible.",
                4,
                day.entries().size());
        assertEquals(
                "Source markers must not collapse delivery or finance into appointments.",
                Set.of(
                        AgendaEntryType.APPOINTMENT,
                        AgendaEntryType.DELIVERY,
                        AgendaEntryType.RECEIVABLE,
                        AgendaEntryType.PAYABLE),
                day.markers());
        assertEquals(
                "Receivable total must retain exact decimal value.",
                new BigDecimal("125.40"),
                day.receivableTotal());
        assertEquals(
                "Payable total must retain exact decimal value.",
                new BigDecimal("30.00"),
                day.payableTotal());
    }

    @Test
    public void settledFinanceUsesSettlementDay() {
        FinanceSummary settled = new FinanceSummary(
                9,
                FinanceEntryType.RECEIVABLE,
                FinanceEntryStatus.SETTLED,
                FinanceEntrySource.MANUAL,
                "Recebimento",
                new BigDecimal("80.00"),
                Optional.of(Instant.parse("2026-08-01T15:00:00Z")),
                Optional.of(Instant.parse("2026-08-05T15:00:00Z")),
                Optional.empty(),
                Optional.empty(),
                Set.of(),
                REFERENCE,
                REFERENCE);

        AgendaMonthSnapshot snapshot = AgendaMonthSnapshot.from(
                YearMonth.of(2026, 8),
                LocalDate.of(2026, 8, 5),
                ZONE,
                List.of(),
                List.of(),
                List.of(settled));

        assertTrue(
                "Settled money must appear on the settlement day.",
                snapshot.selectedDay().markers().contains(
                        AgendaEntryType.RECEIVABLE));
    }

    @Test
    public void replacesLinkedProjectionWithLogisticsAndKeepsLegacyDelivery() {
        AppointmentSummary linked = appointment(
                11,
                AppointmentKind.DELIVERY,
                "2026-08-04T15:00:00Z",
                Optional.of(new AppointmentSummary.Order(
                        501,
                        "material",
                        "confirmed")));
        AppointmentSummary legacy = appointment(
                12,
                AppointmentKind.DELIVERY,
                "2026-08-04T16:00:00Z",
                Optional.of(new AppointmentSummary.Order(
                        502,
                        "material",
                        "confirmed")));

        AgendaMonthSnapshot snapshot = AgendaMonthSnapshot.from(
                YearMonth.of(2026, 8),
                LocalDate.of(2026, 8, 4),
                ZONE,
                List.of(linked, legacy),
                List.of(delivery(601, 501, "2026-08-04T15:00:00Z")),
                List.of());

        List<AgendaEntry> entries = snapshot.selectedDay().entries();
        assertEquals(
                "A linked appointment and logistics record must render once.",
                2,
                entries.size());
        assertEquals(
                "The real logistics record must be the navigable source.",
                AgendaEntrySource.DELIVERY,
                entries.get(0).source());
        assertEquals(
                "Legacy delivery appointments must remain visible.",
                AgendaEntrySource.APPOINTMENT,
                entries.get(1).source());
    }

    private static AppointmentSummary appointment(
            long id,
            AppointmentKind kind,
            String instant) {
        return appointment(id, kind, instant, Optional.empty());
    }

    private static AppointmentSummary appointment(
            long id,
            AppointmentKind kind,
            String instant,
            Optional<AppointmentSummary.Order> order) {
        return new AppointmentSummary(
                id,
                kind,
                AppointmentStatus.SCHEDULED,
                kind == AppointmentKind.DELIVERY
                        ? "Entrega programada"
                        : "Visita técnica",
                Instant.parse(instant),
                60,
                Optional.empty(),
                new AppointmentPerson(
                        10,
                        Optional.of("Responsável"),
                        AppRole.SALESPERSON),
                Optional.empty(),
                Optional.empty(),
                order,
                1,
                kind == AppointmentKind.DELIVERY
                        ? Set.of()
                        : Set.of(AppointmentAction.UPDATE),
                REFERENCE,
                REFERENCE);
    }

    private static DeliverySummary delivery(
            long id,
            long orderId,
            String scheduledAt) {
        return new DeliverySummary(
                id,
                DeliveryStatus.PENDING,
                Set.of(DeliveryAction.START),
                new DeliverySummary.Order(orderId, "confirmed", 2, 1),
                Optional.empty(),
                Optional.of(new DeliverySummary.Customer(
                        81,
                        "Cliente",
                        Optional.empty(),
                        Optional.empty())),
                Optional.empty(),
                Optional.of(Instant.parse(scheduledAt)),
                Optional.empty(),
                REFERENCE);
    }

    private static FinanceSummary finance(
            long id,
            FinanceEntryType type,
            String amount,
            String dueAt) {
        return new FinanceSummary(
                id,
                type,
                FinanceEntryStatus.PENDING,
                FinanceEntrySource.MANUAL,
                "Movimento financeiro",
                new BigDecimal(amount),
                Optional.of(Instant.parse(dueAt)),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Set.of(FinanceAction.SETTLE, FinanceAction.CANCEL),
                REFERENCE,
                REFERENCE);
    }
}
