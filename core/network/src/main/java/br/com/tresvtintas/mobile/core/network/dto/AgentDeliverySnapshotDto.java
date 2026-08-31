package br.com.tresvtintas.mobile.core.network.dto;

import java.util.Set;

public record AgentDeliverySnapshotDto(
        String deliveryStatus,
        String orderStatus) {
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

    public AgentDeliverySnapshotDto {
        if (!DELIVERY_STATUSES.contains(deliveryStatus)
                || !ORDER_STATUSES.contains(orderStatus)) {
            throw new IllegalArgumentException(
                    "Agent delivery snapshot is invalid.");
        }
    }
}
