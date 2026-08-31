package br.com.tresvtintas.mobile.core.order;

import java.math.BigDecimal;

public record OrderConversionResult(long quoteId, int quoteRevision, long orderId,
        int orderRevision, OrderStatus status, BigDecimal total, long deliveryId,
        long sellerUserId, String sellerRole, boolean replayed) {
    public OrderConversionResult {
        if (quoteId < 1 || quoteRevision < 1 || orderId < 1 || orderRevision < 1
                || status != OrderStatus.PENDING || total == null || total.signum() < 0
                || deliveryId < 1 || sellerUserId < 1
                || (!"painter".equals(sellerRole) && !"salesperson".equals(sellerRole))) {
            throw new IllegalArgumentException("Order conversion result is invalid.");
        }
    }
}
