package br.com.tresvtintas.mobile.core.agent;

import br.com.tresvtintas.mobile.core.order.OrderStatus;
import br.com.tresvtintas.mobile.core.order.OrderType;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

public record AgentOrderSummary(
        long orderId,
        AgentOrderOperation operation,
        OrderType orderType,
        Optional<String> organizationName,
        Optional<String> customerName,
        BigDecimal total,
        int itemCount,
        int expectedRevision,
        AgentOrderSnapshot before,
        AgentOrderSnapshot after,
        boolean accessAndRevisionWillBeRevalidated)
        implements AgentActionSummary {
    public AgentOrderSummary {
        operation = Objects.requireNonNull(
                operation,
                "Agent order operation is required.");
        orderType = Objects.requireNonNull(
                orderType,
                "Agent order type is required.");
        organizationName = optionalText(
                organizationName,
                200,
                "Agent order organization is invalid.");
        customerName = optionalText(
                customerName,
                500,
                "Agent order customer is invalid.");
        total = Objects.requireNonNull(
                total,
                "Agent order total is required.");
        before = Objects.requireNonNull(
                before,
                "Agent order previous state is required.");
        after = Objects.requireNonNull(
                after,
                "Agent order next state is required.");
        if (orderId < 1
                || total.signum() < 0
                || total.scale() > 2
                || total.precision() > 10
                || itemCount < 1
                || expectedRevision < 1
                || !accessAndRevisionWillBeRevalidated
                || !validTransition(operation, before, after)) {
            throw new IllegalArgumentException(
                    "Agent order summary is invalid.");
        }
    }

    private static boolean validTransition(
            AgentOrderOperation operation,
            AgentOrderSnapshot before,
            AgentOrderSnapshot after) {
        if (before.paymentStatus() != after.paymentStatus()) {
            return false;
        }
        return switch (operation) {
            case CONFIRM ->
                    before.status() == OrderStatus.PENDING
                            && after.status() == OrderStatus.CONFIRMED;
            case START_FULFILLMENT ->
                    before.status() == OrderStatus.CONFIRMED
                            && after.status() == OrderStatus.IN_PROGRESS;
            case COMPLETE ->
                    before.status() == OrderStatus.IN_PROGRESS
                            && after.status() == OrderStatus.DELIVERED;
        };
    }

    private static Optional<String> optionalText(
            Optional<String> value,
            int maximum,
            String message) {
        Optional<String> normalized = value == null
                ? Optional.empty()
                : value;
        return normalized.map(text -> {
            if (text.isBlank()
                    || text.length() > maximum
                    || text.matches(
                            ".*[\\u0000-\\u0008\\u000b\\u000c\\u000e-\\u001f\\u007f].*")) {
                throw new IllegalArgumentException(message);
            }
            return text;
        });
    }
}
