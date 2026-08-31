package br.com.tresvtintas.mobile.core.network.dto;

import java.util.Set;

public record OrderStatusMutationRequest(
        int expectedRevision,
        String status,
        boolean confirmed) {
    private static final Set<String> TARGETS = Set.of(
            "confirmed",
            "in_progress",
            "delivered");

    public OrderStatusMutationRequest {
        if (expectedRevision < 1 || !TARGETS.contains(status) || !confirmed) {
            throw new IllegalArgumentException(
                    "Order status mutation request is invalid.");
        }
    }
}
