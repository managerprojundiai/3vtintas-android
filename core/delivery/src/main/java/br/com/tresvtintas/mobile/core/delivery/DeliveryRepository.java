package br.com.tresvtintas.mobile.core.delivery;

import java.util.Optional;

public interface DeliveryRepository {
    DeliveryPage page(DeliveryQuery query, Optional<String> cursor)
            throws DeliveryException;

    DeliveryDetail detail(long deliveryId) throws DeliveryException;

    DeliveryMutationResult start(
            long deliveryId,
            int expectedOrderRevision,
            String idempotencyKey) throws DeliveryException;

    DeliveryMutationResult complete(
            long deliveryId,
            int expectedOrderRevision,
            String idempotencyKey) throws DeliveryException;
}
