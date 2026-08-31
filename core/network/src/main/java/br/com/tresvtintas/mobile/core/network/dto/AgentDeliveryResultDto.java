package br.com.tresvtintas.mobile.core.network.dto;

import java.util.Set;

public record AgentDeliveryResultDto(
        long deliveryId,
        String deliveryStatus,
        long orderId,
        String orderStatus,
        int orderRevision,
        boolean changed) {
    private static final Set<String> DELIVERY_STATUSES = Set.of(
            "pending",
            "shipped",
            "in_transit",
            "delivered",
            "failed");
    private static final Set<String> ORDER_STATUSES = Set.of(
            "pending",
            "confirmed",
            "in_progress",
            "delivered",
            "cancelled");

    public AgentDeliveryResultDto {
        DtoValidation.requirePositive(
                deliveryId,
                "Agent delivery result ID");
        DtoValidation.requirePositive(
                orderId,
                "Agent delivery result order ID");
        DtoValidation.requirePositive(
                orderRevision,
                "Agent delivery result revision");
        if (!DELIVERY_STATUSES.contains(deliveryStatus)
                || !ORDER_STATUSES.contains(orderStatus)) {
            throw new IllegalArgumentException(
                    "Agent delivery result is invalid.");
        }
    }
}
