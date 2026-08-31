package br.com.tresvtintas.mobile.core.network.dto;

import java.util.Set;

public record DeliveryRouteStopDto(
        long deliveryId,
        long orderId,
        int orderRevisionSnapshot,
        int position,
        String status,
        String recipientName,
        String normalizedAddress,
        Coordinate coordinate,
        String estimatedArrivalAt,
        int travelDistanceMeters,
        int travelDurationSeconds,
        int serviceDurationSeconds,
        int revision) {
    private static final Set<String> STATUSES = Set.of(
            "planned", "en_route", "arrived", "completed", "skipped");

    public DeliveryRouteStopDto {
        deliveryId = DtoValidation.requirePositive(deliveryId, "Route delivery ID");
        orderId = DtoValidation.requirePositive(orderId, "Route order ID");
        recipientName = DtoValidation.optionalText(
                recipientName, "Route recipient", 500);
        normalizedAddress = DtoValidation.requireText(
                normalizedAddress, "Route stop address", 5_000);
        if (coordinate == null || !STATUSES.contains(status)
                || orderRevisionSnapshot < 1 || position < 1
                || travelDistanceMeters < 0 || travelDurationSeconds < 0
                || serviceDurationSeconds < 1 || revision < 1) {
            throw new IllegalArgumentException("Delivery route stop is invalid.");
        }
        if (estimatedArrivalAt != null) {
            DtoValidation.requireInstant(estimatedArrivalAt, "Route stop estimate");
        }
    }

    public record Coordinate(int latitudeE7, int longitudeE7) {
        private static final int LATITUDE_LIMIT_E7 = 900_000_000;
        private static final int LONGITUDE_LIMIT_E7 = 1_800_000_000;

        public Coordinate {
            if (Math.abs((long) latitudeE7) > LATITUDE_LIMIT_E7
                    || Math.abs((long) longitudeE7) > LONGITUDE_LIMIT_E7) {
                throw new IllegalArgumentException("Route coordinate is invalid.");
            }
        }
    }
}
