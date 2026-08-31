package br.com.tresvtintas.mobile.core.agent;

import br.com.tresvtintas.mobile.core.order.OrderPaymentStatus;
import br.com.tresvtintas.mobile.core.order.OrderStatus;
import java.util.Objects;

public record AgentOrderResult(
        long orderId,
        OrderStatus status,
        int revision,
        OrderPaymentStatus paymentStatus,
        boolean changed) implements AgentActionResult {
    public AgentOrderResult {
        status = Objects.requireNonNull(
                status,
                "Agent order result status is required.");
        paymentStatus = Objects.requireNonNull(
                paymentStatus,
                "Agent order result payment status is required.");
        if (orderId < 1 || revision < 1) {
            throw new IllegalArgumentException(
                    "Agent order result is invalid.");
        }
    }
}
