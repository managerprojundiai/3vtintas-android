package br.com.tresvtintas.mobile.core.network.dto;

public record DeliveryManagementCompletionRequest(
        long organizationId,
        int expectedOrderRevision,
        String confirmation) {
    public DeliveryManagementCompletionRequest {
        organizationId = DtoValidation.requirePositive(
                organizationId,
                "Management organization ID");
        if (expectedOrderRevision < 1
                || !"COMPLETE_DELIVERY_MANAGEMENT".equals(confirmation)) {
            throw new IllegalArgumentException(
                    "Management completion request is invalid.");
        }
    }

    public DeliveryManagementCompletionRequest(
            long organizationId,
            int expectedOrderRevision) {
        this(
                organizationId,
                expectedOrderRevision,
                "COMPLETE_DELIVERY_MANAGEMENT");
    }
}
