package br.com.tresvtintas.mobile.core.delivery;

public record DeliveryMutationResult(
        DeliveryAction action,
        long deliveryId,
        DeliveryStatus deliveryStatus,
        long orderId,
        String orderStatus,
        int orderRevision,
        boolean changed,
        boolean replayed) {
    public DeliveryMutationResult {
        if (action == null || deliveryStatus == null
                || deliveryId < 1 || orderId < 1 || orderRevision < 1
                || orderStatus == null || orderStatus.isBlank()) {
            throw new IllegalArgumentException("Delivery mutation result is invalid.");
        }
    }
}
