package br.com.tresvtintas.mobile.core.network.dto;

import java.util.Set;

public record AgentOrderResultDto(
        long orderId,
        String status,
        int revision,
        String paymentStatus,
        boolean changed) {
    private static final Set<String> STATUSES = Set.of(
            "pending",
            "confirmed",
            "in_progress",
            "delivered",
            "cancelled");

    public AgentOrderResultDto {
        if (orderId < 1
                || !STATUSES.contains(status)
                || revision < 1
                || !Set.of("pending", "received")
                        .contains(paymentStatus)) {
            throw new IllegalArgumentException(
                    "Agent order result is invalid.");
        }
    }
}
