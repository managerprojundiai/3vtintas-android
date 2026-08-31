package br.com.tresvtintas.mobile.data.delivery;

import br.com.tresvtintas.mobile.core.delivery.DeliveryManagementAction;
import br.com.tresvtintas.mobile.core.delivery.DeliveryManagementDetail;
import br.com.tresvtintas.mobile.core.delivery.DeliveryManagementDriver;
import br.com.tresvtintas.mobile.core.delivery.DeliveryManagementMutationResult;
import br.com.tresvtintas.mobile.core.delivery.DeliveryManagementOrganization;
import br.com.tresvtintas.mobile.core.delivery.DeliveryManagementPage;
import br.com.tresvtintas.mobile.core.delivery.DeliveryManagementSummary;
import br.com.tresvtintas.mobile.core.delivery.DeliveryStatus;
import br.com.tresvtintas.mobile.core.network.dto.DeliveryManagementDetailDto;
import br.com.tresvtintas.mobile.core.network.dto.DeliveryManagementDriverDto;
import br.com.tresvtintas.mobile.core.network.dto.DeliveryManagementMutationResponse;
import br.com.tresvtintas.mobile.core.network.dto.DeliveryManagementPageDto;
import br.com.tresvtintas.mobile.core.network.dto.DeliveryManagementSummaryDto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.stream.Collectors;

final class DeliveryManagementDtoMapper {
    private DeliveryManagementDtoMapper() {
        throw new AssertionError("No instances.");
    }

    static DeliveryManagementPage page(DeliveryManagementPageDto value) {
        return new DeliveryManagementPage(
                value.items().stream()
                        .map(DeliveryManagementDtoMapper::summary)
                        .toList(),
                Optional.ofNullable(value.nextCursor()));
    }

    static DeliveryManagementDetail detail(
            DeliveryManagementDetailDto value) {
        DeliveryManagementSummary summary = summary(
                new DeliveryManagementSummaryDto(
                        value.order(),
                        value.organization(),
                        value.customer(),
                        value.delivery(),
                        value.allowedActions()));
        return new DeliveryManagementDetail(
                summary,
                Optional.ofNullable(value.recipient()).map(item ->
                        new DeliveryManagementDetail.Recipient(
                                item.name(),
                                Optional.ofNullable(item.phone()))),
                new DeliveryManagementDetail.Destination(
                        Optional.ofNullable(value.destination().address()),
                        Optional.ofNullable(value.destination().city()),
                        Optional.ofNullable(value.destination().state())),
                Optional.ofNullable(value.instructions()),
                value.items().stream()
                        .map(DeliveryManagementDtoMapper::item)
                        .toList());
    }

    static DeliveryManagementSummary summary(
            DeliveryManagementSummaryDto value) {
        return new DeliveryManagementSummary(
                new DeliveryManagementSummary.Order(
                        value.order().id(),
                        value.order().status(),
                        value.order().revision(),
                        value.order().itemCount(),
                        Instant.parse(value.order().createdAt()),
                        Instant.parse(value.order().updatedAt())),
                new DeliveryManagementOrganization(
                        value.organization().id(),
                        value.organization().name()),
                Optional.ofNullable(value.customer()).map(item ->
                        new DeliveryManagementSummary.Customer(
                                item.id(),
                                item.name(),
                                Optional.ofNullable(item.city()),
                                Optional.ofNullable(item.state()))),
                Optional.ofNullable(value.delivery()).map(item ->
                        new DeliveryManagementSummary.ManagedDelivery(
                                item.id(),
                                enumValue(DeliveryStatus.class, item.status()),
                                optionalInstant(item.scheduledAt()),
                                Optional.ofNullable(item.assignedDriver())
                                        .map(DeliveryManagementDtoMapper::driver))),
                value.allowedActions().stream()
                        .map(item -> enumValue(
                                DeliveryManagementAction.class,
                                item))
                        .collect(Collectors.toUnmodifiableSet()));
    }

    static DeliveryManagementDriver driver(
            DeliveryManagementDriverDto value) {
        return new DeliveryManagementDriver(
                value.userId(),
                Optional.ofNullable(value.name()));
    }

    static DeliveryManagementMutationResult mutation(
            DeliveryManagementMutationResponse value,
            boolean replayed) {
        return new DeliveryManagementMutationResult(
                enumValue(DeliveryManagementAction.class, value.action()),
                value.orderId(),
                optionalLong(value.deliveryId()),
                value.orderStatus(),
                value.orderRevision(),
                optionalInstant(value.scheduledAt()),
                optionalLong(value.assignedDriverUserId()),
                value.changed(),
                replayed);
    }

    private static DeliveryManagementDetail.Item item(
            DeliveryManagementDetailDto.Item value) {
        return new DeliveryManagementDetail.Item(
                value.id(),
                optionalLong(value.productId()),
                value.description(),
                new BigDecimal(value.quantity()),
                Optional.ofNullable(value.unit()));
    }

    private static Optional<Instant> optionalInstant(String value) {
        return value == null
                ? Optional.empty()
                : Optional.of(Instant.parse(value));
    }

    private static OptionalLong optionalLong(Long value) {
        return value == null
                ? OptionalLong.empty()
                : OptionalLong.of(value);
    }

    private static <T extends Enum<T>> T enumValue(
            Class<T> type,
            String value) {
        return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
    }
}
