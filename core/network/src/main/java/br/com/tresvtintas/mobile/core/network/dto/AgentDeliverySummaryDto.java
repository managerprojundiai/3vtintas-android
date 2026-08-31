package br.com.tresvtintas.mobile.core.network.dto;

import java.util.Set;

public record AgentDeliverySummaryDto(
        long deliveryId,
        String operation,
        long orderId,
        String organizationName,
        String customerName,
        String assignedDriverName,
        String scheduledAt,
        int itemCount,
        int expectedOrderRevision,
        AgentDeliverySnapshotDto before,
        AgentDeliverySnapshotDto after,
        boolean assignmentAndRevisionWillBeRevalidated) {
    private static final String START = "start";
    private static final String COMPLETE = "complete";

    public AgentDeliverySummaryDto {
        DtoValidation.requirePositive(
                deliveryId,
                "Agent delivery ID");
        DtoValidation.requirePositive(
                orderId,
                "Agent delivery order ID");
        organizationName = DtoValidation.optionalText(
                organizationName,
                "Agent delivery organization",
                200);
        customerName = DtoValidation.optionalText(
                customerName,
                "Agent delivery customer",
                500);
        assignedDriverName = DtoValidation.requireText(
                assignedDriverName,
                "Agent delivery driver",
                200);
        if (scheduledAt != null) {
            scheduledAt = DtoValidation.requireInstant(
                    scheduledAt,
                    "Agent delivery schedule");
        }
        DtoValidation.requirePositive(
                itemCount,
                "Agent delivery item count");
        DtoValidation.requirePositive(
                expectedOrderRevision,
                "Agent delivery revision");
        if (before == null
                || after == null
                || !assignmentAndRevisionWillBeRevalidated
                || !validTransition(operation, before, after)) {
            throw new IllegalArgumentException(
                    "Agent delivery summary is invalid.");
        }
    }

    private static boolean validTransition(
            String operation,
            AgentDeliverySnapshotDto before,
            AgentDeliverySnapshotDto after) {
        if (START.equals(operation)) {
            return Set.of("pending", "shipped")
                            .contains(before.deliveryStatus())
                    && "confirmed".equals(before.orderStatus())
                    && "in_transit".equals(after.deliveryStatus())
                    && "in_progress".equals(after.orderStatus());
        }
        return COMPLETE.equals(operation)
                && "in_transit".equals(before.deliveryStatus())
                && "in_progress".equals(before.orderStatus())
                && "delivered".equals(after.deliveryStatus())
                && "delivered".equals(after.orderStatus());
    }
}
