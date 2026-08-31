package br.com.tresvtintas.mobile.data.delivery;

import br.com.tresvtintas.mobile.core.delivery.DeliveryAction;
import br.com.tresvtintas.mobile.core.delivery.DeliveryDetail;
import br.com.tresvtintas.mobile.core.delivery.DeliveryPage;
import br.com.tresvtintas.mobile.core.delivery.DeliveryStatus;
import br.com.tresvtintas.mobile.core.delivery.DeliverySummary;
import br.com.tresvtintas.mobile.core.network.dto.DeliveryDetailDto;
import br.com.tresvtintas.mobile.core.network.dto.DeliveryPageDto;
import br.com.tresvtintas.mobile.core.network.dto.DeliverySummaryDto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.stream.Collectors;

final class DeliveryDtoMapper {
    private DeliveryDtoMapper() {
        throw new AssertionError("No instances.");
    }

    static DeliveryPage page(DeliveryPageDto value) {
        return new DeliveryPage(
                value.items().stream().map(DeliveryDtoMapper::summary).toList(),
                Optional.ofNullable(value.nextCursor()));
    }

    static DeliveryDetail detail(DeliveryDetailDto value) {
        DeliverySummaryDto summaryDto = new DeliverySummaryDto(
                value.id(),
                value.status(),
                value.allowedActions(),
                value.order(),
                value.organization(),
                value.customer(),
                value.assignedDriver(),
                value.scheduledAt(),
                value.deliveredAt(),
                value.updatedAt());
        return new DeliveryDetail(
                summary(summaryDto),
                Optional.ofNullable(value.recipient()).map(item ->
                        new DeliveryDetail.Recipient(
                                item.name(),
                                Optional.ofNullable(item.phone()))),
                new DeliveryDetail.Destination(
                        Optional.ofNullable(value.destination().address()),
                        Optional.ofNullable(value.destination().city()),
                        Optional.ofNullable(value.destination().state())),
                Optional.ofNullable(value.instructions()),
                Optional.ofNullable(value.trackingCode()),
                value.items().stream().map(DeliveryDtoMapper::item).toList());
    }

    private static DeliverySummary summary(DeliverySummaryDto value) {
        return new DeliverySummary(
                value.id(),
                enumValue(DeliveryStatus.class, value.status()),
                value.allowedActions().stream()
                        .map(item -> enumValue(DeliveryAction.class, item))
                        .collect(Collectors.toUnmodifiableSet()),
                new DeliverySummary.Order(
                        value.order().id(),
                        value.order().status(),
                        value.order().revision(),
                        value.order().itemCount()),
                Optional.ofNullable(value.organization()).map(item ->
                        new DeliverySummary.Organization(item.id(), item.name())),
                Optional.ofNullable(value.customer()).map(item ->
                        new DeliverySummary.Customer(
                                item.id(),
                                item.name(),
                                Optional.ofNullable(item.city()),
                                Optional.ofNullable(item.state()))),
                Optional.ofNullable(value.assignedDriver()).map(item ->
                        new DeliverySummary.Driver(
                                item.userId(),
                                Optional.ofNullable(item.name()),
                                item.assignedToCurrentActor())),
                optionalInstant(value.scheduledAt()),
                optionalInstant(value.deliveredAt()),
                Instant.parse(value.updatedAt()));
    }

    private static DeliveryDetail.Item item(DeliveryDetailDto.Item value) {
        return new DeliveryDetail.Item(
                value.id(),
                value.productId() == null
                        ? OptionalLong.empty()
                        : OptionalLong.of(value.productId()),
                value.description(),
                new BigDecimal(value.quantity()),
                Optional.ofNullable(value.unit()));
    }

    private static Optional<Instant> optionalInstant(String value) {
        return value == null ? Optional.empty() : Optional.of(Instant.parse(value));
    }

    private static <T extends Enum<T>> T enumValue(
            Class<T> type,
            String value) {
        return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
    }
}
