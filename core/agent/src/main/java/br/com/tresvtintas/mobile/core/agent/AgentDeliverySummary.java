package br.com.tresvtintas.mobile.core.agent;

import br.com.tresvtintas.mobile.core.delivery.DeliveryStatus;
import br.com.tresvtintas.mobile.core.order.OrderStatus;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record AgentDeliverySummary(
        long deliveryId,
        AgentDeliveryOperation operation,
        long orderId,
        Optional<String> organizationName,
        Optional<String> customerName,
        String assignedDriverName,
        Optional<Instant> scheduledAt,
        int itemCount,
        int expectedOrderRevision,
        AgentDeliverySnapshot before,
        AgentDeliverySnapshot after,
        boolean assignmentAndRevisionWillBeRevalidated)
        implements AgentActionSummary {
    public AgentDeliverySummary {
        operation = Objects.requireNonNull(
                operation,
                "Agent delivery operation is required.");
        organizationName = optionalText(
                organizationName,
                200,
                "Agent delivery organization is invalid.");
        customerName = optionalText(
                customerName,
                500,
                "Agent delivery customer is invalid.");
        assignedDriverName = requireText(
                assignedDriverName,
                200,
                "Agent delivery driver is invalid.");
        scheduledAt = scheduledAt == null
                ? Optional.empty()
                : scheduledAt;
        before = Objects.requireNonNull(
                before,
                "Agent delivery previous state is required.");
        after = Objects.requireNonNull(
                after,
                "Agent delivery next state is required.");
        if (deliveryId < 1
                || orderId < 1
                || itemCount < 1
                || expectedOrderRevision < 1
                || !assignmentAndRevisionWillBeRevalidated
                || !validTransition(operation, before, after)) {
            throw new IllegalArgumentException(
                    "Agent delivery summary is invalid.");
        }
    }

    private static boolean validTransition(
            AgentDeliveryOperation operation,
            AgentDeliverySnapshot before,
            AgentDeliverySnapshot after) {
        if (operation == AgentDeliveryOperation.START) {
            return (before.deliveryStatus() == DeliveryStatus.PENDING
                            || before.deliveryStatus()
                                    == DeliveryStatus.SHIPPED)
                    && before.orderStatus() == OrderStatus.CONFIRMED
                    && after.deliveryStatus()
                            == DeliveryStatus.IN_TRANSIT
                    && after.orderStatus() == OrderStatus.IN_PROGRESS;
        }
        return before.deliveryStatus() == DeliveryStatus.IN_TRANSIT
                && before.orderStatus() == OrderStatus.IN_PROGRESS
                && after.deliveryStatus() == DeliveryStatus.DELIVERED
                && after.orderStatus() == OrderStatus.DELIVERED;
    }

    private static Optional<String> optionalText(
            Optional<String> value,
            int maximum,
            String message) {
        Optional<String> normalized = value == null
                ? Optional.empty()
                : value;
        return normalized.map(text ->
                requireText(text, maximum, message));
    }

    private static String requireText(
            String value,
            int maximum,
            String message) {
        if (value == null
                || value.isBlank()
                || value.length() > maximum
                || value.matches(
                        ".*[\\u0000-\\u0008\\u000b\\u000c\\u000e-\\u001f\\u007f].*")) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
