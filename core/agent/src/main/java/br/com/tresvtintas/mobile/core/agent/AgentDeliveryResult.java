package br.com.tresvtintas.mobile.core.agent;

import br.com.tresvtintas.mobile.core.delivery.DeliveryStatus;
import br.com.tresvtintas.mobile.core.order.OrderStatus;
import java.util.Objects;

public record AgentDeliveryResult(
        long deliveryId,
        DeliveryStatus deliveryStatus,
        long orderId,
        OrderStatus orderStatus,
        int orderRevision,
        boolean changed) implements AgentActionResult {
    public AgentDeliveryResult {
        deliveryStatus = Objects.requireNonNull(
                deliveryStatus,
                "Agent delivery result status is required.");
        orderStatus = Objects.requireNonNull(
                orderStatus,
                "Agent delivery result order status is required.");
        if (deliveryId < 1 || orderId < 1 || orderRevision < 1) {
            throw new IllegalArgumentException(
                    "Agent delivery result is invalid.");
        }
    }
}
