package br.com.tresvtintas.mobile.core.network.dto;

import java.util.Set;

public record DeliveryMutationResponse(
        String action,
        long deliveryId,
        String deliveryStatus,
        long orderId,
        String orderStatus,
        int orderRevision,
        boolean changed) {
    public DeliveryMutationResponse {
        if (!Set.of("start", "complete").contains(action)
                || deliveryId < 1
                || !Set.of(
                        "pending",
                        "shipped",
                        "in_transit",
                        "delivered",
                        "failed").contains(deliveryStatus)
                || orderId < 1
                || !Set.of(
                        "pending",
                        "confirmed",
                        "in_progress",
                        "delivered",
                        "cancelled").contains(orderStatus)
                || orderRevision < 1) {
            throw new IllegalArgumentException(
                    "Delivery mutation response is invalid.");
        }
    }
}
