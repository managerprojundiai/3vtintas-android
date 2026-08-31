package br.com.tresvtintas.mobile.core.network.dto;

import java.util.Set;

public record AgentOrderSummaryDto(
        long orderId,
        String operation,
        String orderType,
        String organizationName,
        String customerName,
        String total,
        int itemCount,
        int expectedRevision,
        AgentOrderSnapshotDto before,
        AgentOrderSnapshotDto after,
        boolean accessAndRevisionWillBeRevalidated) {
    private static final Set<String> OPERATIONS = Set.of(
            "confirm",
            "start_fulfillment",
            "complete");

    public AgentOrderSummaryDto {
        organizationName = DtoValidation.optionalText(
                organizationName,
                "Agent order organization",
                200);
        customerName = DtoValidation.optionalText(
                customerName,
                "Agent order customer",
                500);
        if (orderId < 1
                || !OPERATIONS.contains(operation)
                || !Set.of("material", "labor").contains(orderType)
                || total == null
                || !total.matches("^\\d{1,8}\\.\\d{2}$")
                || itemCount < 1
                || expectedRevision < 1
                || before == null
                || after == null
                || !accessAndRevisionWillBeRevalidated
                || !validTransition(operation, before, after)) {
            throw new IllegalArgumentException(
                    "Agent order summary is invalid.");
        }
    }

    private static boolean validTransition(
            String operation,
            AgentOrderSnapshotDto before,
            AgentOrderSnapshotDto after) {
        if (!before.paymentStatus().equals(after.paymentStatus())) {
            return false;
        }
        return switch (operation) {
            case "confirm" ->
                    "pending".equals(before.status())
                            && "confirmed".equals(after.status());
            case "start_fulfillment" ->
                    "confirmed".equals(before.status())
                            && "in_progress".equals(after.status());
            case "complete" ->
                    "in_progress".equals(before.status())
                            && "delivered".equals(after.status());
            default -> false;
        };
    }
}
