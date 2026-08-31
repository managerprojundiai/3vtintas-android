package br.com.tresvtintas.mobile.core.delivery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.Test;

public final class DeliveryRoutePlanTest {
    @Test
    public void choosesActiveRouteAndFirstUnfinishedStop() {
        DeliveryRoutePlan completed = plan(
                "00000000-0000-4000-8000-000000000901",
                DeliveryRouteStatus.COMPLETED,
                Instant.parse("2026-08-03T11:00:00Z"));
        DeliveryRoutePlan active = plan(
                "00000000-0000-4000-8000-000000000902",
                DeliveryRouteStatus.IN_PROGRESS,
                Instant.parse("2026-08-03T12:00:00Z"));

        DeliveryRoutePlan selected = new DeliveryRoutePage(
                List.of(completed, active)).current().orElseThrow();

        assertEquals(
                "The active route must take precedence over completed history.",
                active.routeKey(),
                selected.routeKey());
        assertEquals(
                "Navigation must skip terminal stops.",
                82L,
                selected.nextStop().orElseThrow().deliveryId());
    }

    @Test
    public void rejectsNonContiguousOrDuplicateStopOrder() {
        List<DeliveryRouteStop> invalid = List.of(
                stop(81, 1, DeliveryRouteStopStatus.COMPLETED),
                stop(81, 2, DeliveryRouteStopStatus.PLANNED));

        assertThrows(
                "Duplicate delivery identifiers must fail closed.",
                IllegalArgumentException.class,
                () -> plan(
                        "00000000-0000-4000-8000-000000000903",
                        DeliveryRouteStatus.CONFIRMED,
                        Instant.parse("2026-08-03T12:00:00Z"),
                        invalid));
    }

    static DeliveryRoutePlan plan(
            String key,
            DeliveryRouteStatus status,
            Instant startsAt) {
        return plan(
                key,
                status,
                startsAt,
                List.of(
                        stop(81, 1, DeliveryRouteStopStatus.COMPLETED),
                        stop(82, 2, DeliveryRouteStopStatus.PLANNED)));
    }

    private static DeliveryRoutePlan plan(
            String key,
            DeliveryRouteStatus status,
            Instant startsAt,
            List<DeliveryRouteStop> stops) {
        return new DeliveryRoutePlan(
                key,
                new DeliveryRoutePlan.Organization(2, "3V Tintas Jundiaí"),
                new DeliveryRoutePlan.Driver(
                        OptionalLong.of(21),
                        Optional.of("Yasmin")),
                LocalDate.of(2026, 8, 3),
                startsAt,
                status,
                true,
                new DeliveryRoutePlan.Place(
                        "Loja Jundiaí",
                        new DeliveryRouteCoordinate(-231000000, -469000000)),
                Optional.empty(),
                12_300,
                2_100,
                2,
                Instant.parse("2026-08-03T13:00:00Z"),
                Optional.of(Instant.parse("2026-08-03T10:00:00Z")),
                Optional.empty(),
                stops);
    }

    private static DeliveryRouteStop stop(
            long deliveryId,
            int position,
            DeliveryRouteStopStatus status) {
        return new DeliveryRouteStop(
                deliveryId,
                700 + deliveryId,
                3,
                position,
                status,
                Optional.of("Cliente " + deliveryId),
                "Rua de teste, " + position,
                new DeliveryRouteCoordinate(-231000000 + position, -469000000),
                Optional.of(Instant.parse("2026-08-03T12:30:00Z")),
                1_000,
                600,
                600,
                1);
    }
}
