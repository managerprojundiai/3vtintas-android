package br.com.tresvtintas.mobile.core.order;

import java.util.Optional;

public interface OrderRepository {
    OrderPage page(OrderQuery query, Optional<String> cursor) throws OrderException;

    OrderDetail detail(long orderId) throws OrderException;

    OrderConversionResult convertMaterialQuote(long quoteId, int expectedRevision,
            String idempotencyKey) throws OrderException;

    OrderActionResult transitionStatus(
            long orderId,
            int expectedRevision,
            OrderStatus status,
            String idempotencyKey) throws OrderException;

    OrderActionResult cancel(
            long orderId,
            int expectedRevision,
            Optional<String> reason,
            String idempotencyKey) throws OrderException;

    OrderActionResult recordPayment(
            long orderId,
            int expectedRevision,
            OrderPaymentMethod paymentMethod,
            Optional<String> paymentReference,
            String idempotencyKey) throws OrderException;
}
