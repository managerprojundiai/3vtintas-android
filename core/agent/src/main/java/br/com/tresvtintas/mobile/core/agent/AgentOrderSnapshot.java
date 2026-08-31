package br.com.tresvtintas.mobile.core.agent;

import br.com.tresvtintas.mobile.core.order.OrderPaymentStatus;
import br.com.tresvtintas.mobile.core.order.OrderStatus;
import java.util.Objects;

public record AgentOrderSnapshot(
        OrderStatus status,
        OrderPaymentStatus paymentStatus) {
    public AgentOrderSnapshot {
        status = Objects.requireNonNull(
                status,
                "Agent order status is required.");
        paymentStatus = Objects.requireNonNull(
                paymentStatus,
                "Agent order payment status is required.");
    }
}
