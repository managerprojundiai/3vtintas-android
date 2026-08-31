package br.com.tresvtintas.mobile.core.network.dto;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record DeliveryRoutePlanDto(
        String routeKey,
        Organization organization,
        Driver driver,
        String serviceDate,
        String startsAt,
        String status,
        boolean returnToOrigin,
        Place origin,
        Place destination,
        int totalDistanceMeters,
        int totalTravelDurationSeconds,
        int revision,
        String expiresAt,
        String confirmedAt,
        String cancelledAt,
        List<DeliveryRouteStopDto> stops) {
    private static final Set<String> STATUSES = Set.of(
            "draft", "confirmed", "in_progress", "completed", "cancelled");

    public DeliveryRoutePlanDto {
        routeKey = DtoValidation.requireUuid(routeKey, "Delivery route key");
        serviceDate = DtoValidation.requireDate(serviceDate, "Route service date");
        startsAt = DtoValidation.requireInstant(startsAt, "Route start");
        expiresAt = DtoValidation.requireInstant(expiresAt, "Route expiration");
        if (confirmedAt != null) {
            DtoValidation.requireInstant(confirmedAt, "Route confirmation");
        }
        if (cancelledAt != null) {
            DtoValidation.requireInstant(cancelledAt, "Route cancellation");
        }
        if (organization == null || driver == null || origin == null
                || !STATUSES.contains(status) || totalDistanceMeters < 0
                || totalTravelDurationSeconds < 0 || revision < 1
                || stops == null || stops.size() < 2 || stops.size() > 25
                || stops.stream().anyMatch(java.util.Objects::isNull)
                || returnToOrigin && destination != null) {
            throw new IllegalArgumentException("Delivery route plan is invalid.");
        }
        stops = List.copyOf(stops);
        validateStops(stops);
    }

    private static void validateStops(List<DeliveryRouteStopDto> values) {
        Set<Long> identifiers = new HashSet<>();
        for (int index = 0; index < values.size(); index++) {
            DeliveryRouteStopDto stop = values.get(index);
            if (stop.position() != index + 1 || !identifiers.add(stop.deliveryId())) {
                throw new IllegalArgumentException("Delivery route stop order is invalid.");
            }
        }
    }

    public record Organization(long id, String name) {
        public Organization {
            id = DtoValidation.requirePositive(id, "Route organization ID");
            name = DtoValidation.requireText(name, "Route organization", 200);
        }
    }

    public record Driver(Long userId, String name) {
        public Driver {
            userId = DtoValidation.optionalPositive(userId, "Route driver ID");
            name = DtoValidation.optionalText(name, "Route driver", 500);
        }
    }

    public record Place(String address, DeliveryRouteStopDto.Coordinate coordinate) {
        public Place {
            address = DtoValidation.requireText(address, "Route place", 5_000);
            if (coordinate == null) {
                throw new IllegalArgumentException("Route place coordinate is required.");
            }
        }
    }
}
