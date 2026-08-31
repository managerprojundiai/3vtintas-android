package br.com.tresvtintas.mobile.core.network.dto;

import java.util.Set;

public record OrderMutationResponse(
        String action,
        long orderId,
        int revision,
        String status,
        String paymentStatus,
        boolean changed) {
    private static final Set<String> ACTIONS = Set.of(
            "confirm",
            "start_fulfillment",
            "complete",
            "cancel",
            "record_payment");
    private static final Set<String> STATUSES = Set.of(
            "pending",
            "confirmed",
            "in_progress",
            "delivered",
            "cancelled");
    private static final Set<String> PAYMENT_STATUSES = Set.of(
            "pending",
            "received");

    public OrderMutationResponse {
        if (!ACTIONS.contains(action)
                || orderId < 1
                || revision < 1
                || !STATUSES.contains(status)
                || !PAYMENT_STATUSES.contains(paymentStatus)) {
            throw new IllegalArgumentException("Order mutation response is invalid.");
        }
    }
}
