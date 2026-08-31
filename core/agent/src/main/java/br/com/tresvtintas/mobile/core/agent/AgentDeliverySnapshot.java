package br.com.tresvtintas.mobile.core.agent;

import br.com.tresvtintas.mobile.core.delivery.DeliveryStatus;
import br.com.tresvtintas.mobile.core.order.OrderStatus;
import java.util.Objects;

public record AgentDeliverySnapshot(
        DeliveryStatus deliveryStatus,
        OrderStatus orderStatus) {
    public AgentDeliverySnapshot {
        deliveryStatus = Objects.requireNonNull(
                deliveryStatus,
                "Agent delivery status is required.");
        orderStatus = Objects.requireNonNull(
                orderStatus,
                "Agent delivery order status is required.");
    }
}
