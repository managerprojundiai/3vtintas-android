package br.com.tresvtintas.mobile.core.network.dto;

public record DeliveryStartRequest(
        int expectedOrderRevision,
        String confirmation) {
    public DeliveryStartRequest(int expectedOrderRevision) {
        this(expectedOrderRevision, "START_DELIVERY");
    }

    public DeliveryStartRequest {
        if (expectedOrderRevision < 1
                || !"START_DELIVERY".equals(confirmation)) {
            throw new IllegalArgumentException("Delivery start request is invalid.");
        }
    }
}
