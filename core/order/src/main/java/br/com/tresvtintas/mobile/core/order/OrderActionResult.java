package br.com.tresvtintas.mobile.core.order;

public record OrderActionResult(
        OrderAction action,
        long orderId,
        int revision,
        OrderStatus status,
        OrderPaymentStatus paymentStatus,
        boolean changed,
        boolean replayed) {
    public OrderActionResult {
        if (action == null
                || orderId < 1
                || revision < 1
                || status == null
                || paymentStatus == null) {
            throw new IllegalArgumentException("Order action result is invalid.");
        }
    }
}
