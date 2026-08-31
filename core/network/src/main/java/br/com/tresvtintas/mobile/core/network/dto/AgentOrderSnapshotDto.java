package br.com.tresvtintas.mobile.core.network.dto;

import java.util.Set;

public record AgentOrderSnapshotDto(
        String status,
        String paymentStatus) {
    private static final Set<String> STATUSES = Set.of(
            "pending",
            "confirmed",
            "in_progress",
            "delivered",
            "cancelled");

    public AgentOrderSnapshotDto {
        if (!STATUSES.contains(status)
                || !Set.of("pending", "received")
                        .contains(paymentStatus)) {
            throw new IllegalArgumentException(
                    "Agent order snapshot is invalid.");
        }
    }
}
