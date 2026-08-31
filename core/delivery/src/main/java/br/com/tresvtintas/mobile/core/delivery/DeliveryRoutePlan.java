package br.com.tresvtintas.mobile.core.delivery;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;

public record DeliveryRoutePlan(
        String routeKey,
        Organization organization,
        Driver driver,
        LocalDate serviceDate,
        Instant startsAt,
        DeliveryRouteStatus status,
        boolean returnToOrigin,
        Place origin,
        Optional<Place> destination,
        int totalDistanceMeters,
        int totalTravelDurationSeconds,
        int revision,
        Instant expiresAt,
        Optional<Instant> confirmedAt,
        Optional<Instant> cancelledAt,
        List<DeliveryRouteStop> stops) {
    public DeliveryRoutePlan {
        validateRouteKey(routeKey);
        Objects.requireNonNull(organization, "Route organization is required.");
        Objects.requireNonNull(driver, "Route driver is required.");
        Objects.requireNonNull(serviceDate, "Route service date is required.");
        Objects.requireNonNull(startsAt, "Route start is required.");
        Objects.requireNonNull(status, "Route status is required.");
        Objects.requireNonNull(origin, "Route origin is required.");
        destination = Objects.requireNonNull(
                destination, "Route destination is required.");
        Objects.requireNonNull(expiresAt, "Route expiration is required.");
        confirmedAt = Objects.requireNonNull(
                confirmedAt, "Route confirmation is required.");
        cancelledAt = Objects.requireNonNull(
                cancelledAt, "Route cancellation is required.");
        Objects.requireNonNull(stops, "Route stops are required.");
        if (totalDistanceMeters < 0 || totalTravelDurationSeconds < 0
                || revision < 1 || stops.size() < 2 || stops.size() > 25
                || stops.stream().anyMatch(Objects::isNull)
                || returnToOrigin && destination.isPresent()) {
            throw new IllegalArgumentException("Delivery route plan is invalid.");
        }
        stops = List.copyOf(stops);
        validateStops(stops);
    }

    public Optional<DeliveryRouteStop> nextStop() {
        return stops.stream().filter(stop -> !stop.status().isTerminal()).findFirst();
    }

    private static void validateRouteKey(String value) {
        try {
            if (value == null || !UUID.fromString(value).toString().equals(value)) {
                throw new IllegalArgumentException("Delivery route key is invalid.");
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Delivery route key is invalid.", exception);
        }
    }

    private static void validateStops(List<DeliveryRouteStop> values) {
        Set<Long> deliveryIds = new HashSet<>();
        for (int index = 0; index < values.size(); index++) {
            DeliveryRouteStop stop = values.get(index);
            if (stop.position() != index + 1 || !deliveryIds.add(stop.deliveryId())) {
                throw new IllegalArgumentException("Delivery route stop order is invalid.");
            }
        }
    }

    public record Organization(long id, String name) {
        public Organization {
            if (id < 1 || name == null || name.isBlank() || name.length() > 200) {
                throw new IllegalArgumentException("Route organization is invalid.");
            }
        }
    }

    public record Driver(OptionalLong userId, Optional<String> name) {
        public Driver {
            userId = userId == null ? OptionalLong.empty() : userId;
            name = Objects.requireNonNull(name, "Route driver name is required.");
            if (userId.isPresent() && userId.getAsLong() < 1
                    || name.filter(value -> value.length() > 500).isPresent()) {
                throw new IllegalArgumentException("Route driver is invalid.");
            }
        }
    }

    public record Place(String address, DeliveryRouteCoordinate coordinate) {
        public Place {
            Objects.requireNonNull(coordinate, "Route place coordinate is required.");
            if (address == null || address.isBlank() || address.length() > 5_000) {
                throw new IllegalArgumentException("Route place is invalid.");
            }
        }
    }
}
