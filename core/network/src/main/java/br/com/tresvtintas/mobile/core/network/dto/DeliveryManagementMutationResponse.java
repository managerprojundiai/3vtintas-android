package br.com.tresvtintas.mobile.core.network.dto;

import java.util.Set;

public record DeliveryManagementMutationResponse(
        String action,
        long orderId,
        Long deliveryId,
        String orderStatus,
        int orderRevision,
        String scheduledAt,
        Long assignedDriverUserId,
        boolean changed) {
    public DeliveryManagementMutationResponse {
        if (!Set.of("schedule", "assign", "unassign", "complete")
                        .contains(action)
                || orderId < 1
                || orderRevision < 1
                || !Set.of(
                        "pending", "confirmed", "in_progress",
                        "delivered", "cancelled")
                        .contains(orderStatus)) {
            throw new IllegalArgumentException(
                    "Management delivery mutation is invalid.");
        }
        deliveryId = DtoValidation.optionalPositive(
                deliveryId,
                "Management delivery ID");
        assignedDriverUserId = DtoValidation.optionalPositive(
                assignedDriverUserId,
                "Management driver ID");
        if (scheduledAt != null) {
            scheduledAt = DtoValidation.requireInstant(
                    scheduledAt,
                    "Management schedule");
        }
    }
}
