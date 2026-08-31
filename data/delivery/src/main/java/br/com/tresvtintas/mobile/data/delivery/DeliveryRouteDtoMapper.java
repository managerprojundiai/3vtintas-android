package br.com.tresvtintas.mobile.data.delivery;

import br.com.tresvtintas.mobile.core.delivery.DeliveryRouteCoordinate;
import br.com.tresvtintas.mobile.core.delivery.DeliveryRoutePage;
import br.com.tresvtintas.mobile.core.delivery.DeliveryRoutePlan;
import br.com.tresvtintas.mobile.core.delivery.DeliveryRouteStatus;
import br.com.tresvtintas.mobile.core.delivery.DeliveryRouteStop;
import br.com.tresvtintas.mobile.core.delivery.DeliveryRouteStopStatus;
import br.com.tresvtintas.mobile.core.network.dto.DeliveryRoutePageDto;
import br.com.tresvtintas.mobile.core.network.dto.DeliveryRoutePlanDto;
import br.com.tresvtintas.mobile.core.network.dto.DeliveryRouteStopDto;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;

final class DeliveryRouteDtoMapper {
    private DeliveryRouteDtoMapper() {
        throw new AssertionError("No instances.");
    }

    static DeliveryRoutePage page(DeliveryRoutePageDto value) {
        return new DeliveryRoutePage(value.items().stream()
                .map(DeliveryRouteDtoMapper::plan)
                .toList());
    }

    static DeliveryRoutePlan plan(DeliveryRoutePlanDto value) {
        return new DeliveryRoutePlan(
                value.routeKey(),
                new DeliveryRoutePlan.Organization(
                        value.organization().id(),
                        value.organization().name()),
                driver(value.driver()),
                LocalDate.parse(value.serviceDate()),
                Instant.parse(value.startsAt()),
                enumValue(DeliveryRouteStatus.class, value.status()),
                value.returnToOrigin(),
                place(value.origin()),
                Optional.ofNullable(value.destination()).map(
                        DeliveryRouteDtoMapper::place),
                value.totalDistanceMeters(),
                value.totalTravelDurationSeconds(),
                value.revision(),
                Instant.parse(value.expiresAt()),
                optionalInstant(value.confirmedAt()),
                optionalInstant(value.cancelledAt()),
                value.stops().stream().map(DeliveryRouteDtoMapper::stop).toList());
    }

    private static DeliveryRoutePlan.Driver driver(DeliveryRoutePlanDto.Driver value) {
        return new DeliveryRoutePlan.Driver(
                value.userId() == null
                        ? OptionalLong.empty()
                        : OptionalLong.of(value.userId()),
                Optional.ofNullable(value.name()));
    }

    private static DeliveryRoutePlan.Place place(DeliveryRoutePlanDto.Place value) {
        return new DeliveryRoutePlan.Place(
                value.address(),
                coordinate(value.coordinate()));
    }

    private static DeliveryRouteStop stop(DeliveryRouteStopDto value) {
        return new DeliveryRouteStop(
                value.deliveryId(),
                value.orderId(),
                value.orderRevisionSnapshot(),
                value.position(),
                enumValue(DeliveryRouteStopStatus.class, value.status()),
                Optional.ofNullable(value.recipientName()),
                value.normalizedAddress(),
                coordinate(value.coordinate()),
                optionalInstant(value.estimatedArrivalAt()),
                value.travelDistanceMeters(),
                value.travelDurationSeconds(),
                value.serviceDurationSeconds(),
                value.revision());
    }

    private static DeliveryRouteCoordinate coordinate(
            DeliveryRouteStopDto.Coordinate value) {
        return new DeliveryRouteCoordinate(
                value.latitudeE7(),
                value.longitudeE7());
    }

    private static Optional<Instant> optionalInstant(String value) {
        return value == null ? Optional.empty() : Optional.of(Instant.parse(value));
    }

    private static <T extends Enum<T>> T enumValue(Class<T> type, String value) {
        return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
    }
}
