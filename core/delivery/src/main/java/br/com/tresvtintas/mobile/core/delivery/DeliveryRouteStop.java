package br.com.tresvtintas.mobile.core.delivery;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record DeliveryRouteStop(
        long deliveryId,
        long orderId,
        int orderRevisionSnapshot,
        int position,
        DeliveryRouteStopStatus status,
        Optional<String> recipientName,
        String address,
        DeliveryRouteCoordinate coordinate,
        Optional<Instant> estimatedArrivalAt,
        int travelDistanceMeters,
        int travelDurationSeconds,
        int serviceDurationSeconds,
        int revision) {
    public DeliveryRouteStop {
        recipientName = Objects.requireNonNull(
                recipientName, "Route recipient is required.");
        estimatedArrivalAt = Objects.requireNonNull(
                estimatedArrivalAt, "Route estimate is required.");
        Objects.requireNonNull(status, "Route stop status is required.");
        Objects.requireNonNull(coordinate, "Route stop coordinate is required.");
        if (deliveryId < 1 || orderId < 1 || orderRevisionSnapshot < 1
                || position < 1 || revision < 1
                || travelDistanceMeters < 0 || travelDurationSeconds < 0
                || serviceDurationSeconds < 1 || address == null
                || address.isBlank() || address.length() > 5_000
                || recipientName.filter(value -> value.length() > 500).isPresent()) {
            throw new IllegalArgumentException("Delivery route stop is invalid.");
        }
    }
}
