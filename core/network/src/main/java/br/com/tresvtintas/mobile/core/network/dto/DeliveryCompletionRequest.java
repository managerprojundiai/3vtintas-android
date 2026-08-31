package br.com.tresvtintas.mobile.core.network.dto;

public record DeliveryCompletionRequest(
        int expectedOrderRevision,
        String confirmation) {
    public DeliveryCompletionRequest(int expectedOrderRevision) {
        this(expectedOrderRevision, "COMPLETE_DELIVERY");
    }

    public DeliveryCompletionRequest {
        if (expectedOrderRevision < 1
                || !"COMPLETE_DELIVERY".equals(confirmation)) {
            throw new IllegalArgumentException(
                    "Delivery completion request is invalid.");
        }
    }
}
