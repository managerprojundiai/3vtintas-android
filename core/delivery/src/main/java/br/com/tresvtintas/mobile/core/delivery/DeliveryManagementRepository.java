package br.com.tresvtintas.mobile.core.delivery;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

public interface DeliveryManagementRepository {
    List<DeliveryManagementOrganization> organizations()
            throws DeliveryException;

    List<DeliveryManagementDriver> drivers(long organizationId)
            throws DeliveryException;

    DeliveryManagementPage page(
            DeliveryManagementQuery query,
            Optional<String> cursor) throws DeliveryException;

    DeliveryManagementDetail detail(long organizationId, long orderId)
            throws DeliveryException;

    DeliveryManagementMutationResult schedule(
            long organizationId,
            long orderId,
            int expectedOrderRevision,
            Instant scheduledAt,
            int durationMinutes,
            String idempotencyKey) throws DeliveryException;

    DeliveryManagementMutationResult assign(
            long organizationId,
            long orderId,
            int expectedOrderRevision,
            OptionalLong driverUserId,
            String idempotencyKey) throws DeliveryException;

    DeliveryManagementMutationResult complete(
            long organizationId,
            long orderId,
            int expectedOrderRevision,
            String idempotencyKey) throws DeliveryException;
}
