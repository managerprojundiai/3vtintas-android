package br.com.tresvtintas.mobile.core.delivery;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

public record DeliveryManagementMutationResult(
        DeliveryManagementAction action,
        long orderId,
        OptionalLong deliveryId,
        String orderStatus,
        int orderRevision,
        Optional<Instant> scheduledAt,
        OptionalLong assignedDriverUserId,
        boolean changed,
        boolean replayed) {
    public DeliveryManagementMutationResult {
        Objects.requireNonNull(action, "Management action is required.");
        if (orderId < 1 || orderRevision < 1
                || orderStatus == null || orderStatus.isBlank()) {
            throw new IllegalArgumentException(
                    "Delivery management mutation is invalid.");
        }
        deliveryId = Objects.requireNonNull(deliveryId, "Delivery ID is required.");
        scheduledAt = Objects.requireNonNull(scheduledAt, "Schedule is required.");
        assignedDriverUserId = Objects.requireNonNull(
                assignedDriverUserId,
                "Driver ID is required.");
    }
}
