package br.com.tresvtintas.mobile.data.order;

import br.com.tresvtintas.mobile.core.network.dto.OrderDetailDto;
import br.com.tresvtintas.mobile.core.network.dto.OrderItemDto;
import br.com.tresvtintas.mobile.core.network.dto.OrderPageDto;
import br.com.tresvtintas.mobile.core.network.dto.OrderPricingSnapshotDto;
import br.com.tresvtintas.mobile.core.network.dto.OrderSummaryDto;
import br.com.tresvtintas.mobile.core.order.OrderDeliveryStatus;
import br.com.tresvtintas.mobile.core.order.OrderAction;
import br.com.tresvtintas.mobile.core.order.OrderDetail;
import br.com.tresvtintas.mobile.core.order.OrderItem;
import br.com.tresvtintas.mobile.core.order.OrderPage;
import br.com.tresvtintas.mobile.core.order.OrderPaymentStatus;
import br.com.tresvtintas.mobile.core.order.OrderPricingSnapshot;
import br.com.tresvtintas.mobile.core.order.OrderStatus;
import br.com.tresvtintas.mobile.core.order.OrderSummary;
import br.com.tresvtintas.mobile.core.order.OrderType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.OptionalInt;
import java.util.stream.Collectors;

final class OrderDtoMapper {
    private OrderDtoMapper() { }

    static OrderPage page(OrderPageDto value, boolean canReadPrices) {
        return new OrderPage(value.items().stream()
                .map(item -> summary(item, canReadPrices)).toList(),
                Optional.ofNullable(value.nextCursor()));
    }

    static OrderDetail detail(OrderDetailDto value, boolean canReadPrices) {
        OrderSummaryDto summaryDto = new OrderSummaryDto(value.id(), value.type(), value.status(),
                value.revision(), value.paymentStatus(), value.allowedActions(),
                value.total(), value.itemCount(),
                value.quoteId(), value.organization(), value.seller(), value.customer(),
                value.delivery(), value.createdAt(), value.updatedAt());
        requirePricingVisibility(value.pricing(), canReadPrices);
        return new OrderDetail(summary(summaryDto, canReadPrices), Optional.ofNullable(value.notes()),
                Optional.ofNullable(value.pricing()).map(OrderDtoMapper::pricing),
                Optional.ofNullable(value.customerContact()).map(OrderDtoMapper::contact),
                Optional.ofNullable(value.deliveryDetails()).map(OrderDtoMapper::deliveryDetails),
                value.items().stream().map(item -> item(item, canReadPrices)).toList());
    }

    private static OrderSummary summary(OrderSummaryDto value, boolean canReadPrices) {
        requireMoneyVisibility(value.total(), canReadPrices);
        return new OrderSummary(value.id(), enumValue(OrderType.class, value.type()),
                enumValue(OrderStatus.class, value.status()), value.revision(),
                enumValue(OrderPaymentStatus.class, value.paymentStatus()),
                value.allowedActions().stream()
                        .map(item -> enumValue(OrderAction.class, item))
                        .collect(Collectors.toUnmodifiableSet()),
                optionalMoney(value.total()), value.itemCount(), optionalLong(value.quoteId()),
                Optional.ofNullable(value.organization()).map(item ->
                        new OrderSummary.Organization(item.id(), item.name())),
                Optional.ofNullable(value.seller()).map(item ->
                        new OrderSummary.Seller(item.userId(), item.role(), Optional.ofNullable(item.name()))),
                Optional.ofNullable(value.customer()).map(item ->
                        new OrderSummary.Customer(item.id(), item.name())),
                Optional.ofNullable(value.delivery()).map(item -> new OrderSummary.Delivery(
                        item.id(), enumValue(OrderDeliveryStatus.class, item.status()),
                        optionalInstant(item.estimatedAt()), item.assignedToCurrentActor())),
                Instant.parse(value.createdAt()), Instant.parse(value.updatedAt()));
    }

    private static OrderItem item(OrderItemDto value, boolean canReadPrices) {
        requireMoneyVisibility(value.unitPrice(), canReadPrices);
        requireMoneyVisibility(value.total(), canReadPrices);
        return new OrderItem(value.id(), optionalLong(value.productId()), value.description(),
                new BigDecimal(value.quantity()), Optional.ofNullable(value.unit()),
                optionalMoney(value.unitPrice()), optionalMoney(value.total()));
    }

    private static OrderPricingSnapshot pricing(OrderPricingSnapshotDto value) {
        boolean resolved = "RESOLVED".equals(value.state());
        return new OrderPricingSnapshot(resolved,
                Optional.ofNullable(value.priceListCode()),
                Optional.ofNullable(value.priceListName()),
                Optional.ofNullable(value.priceListVersionPublicId()),
                optionalInt(value.priceListVersionNumber()),
                optionalInt(value.policyRevision()),
                Optional.ofNullable(value.selectionMode()),
                optionalInstant(value.resolvedAt()));
    }

    private static OrderDetail.CustomerContact contact(OrderDetailDto.CustomerContact value) {
        return new OrderDetail.CustomerContact(value.id(), value.name(), Optional.ofNullable(value.email()),
                Optional.ofNullable(value.phone()), Optional.ofNullable(value.address()),
                Optional.ofNullable(value.city()), Optional.ofNullable(value.state()));
    }

    private static OrderDetail.DeliveryDetails deliveryDetails(OrderDetailDto.DeliveryDetails value) {
        return new OrderDetail.DeliveryDetails(Optional.ofNullable(value.address()),
                Optional.ofNullable(value.notes()), Optional.ofNullable(value.trackingCode()),
                optionalInstant(value.estimatedAt()), optionalInstant(value.deliveredAt()));
    }

    private static OptionalLong optionalLong(Long value) {
        return value == null ? OptionalLong.empty() : OptionalLong.of(value);
    }

    private static OptionalInt optionalInt(Integer value) {
        return value == null ? OptionalInt.empty() : OptionalInt.of(value);
    }

    private static Optional<BigDecimal> optionalMoney(String value) {
        return value == null ? Optional.empty() : Optional.of(new BigDecimal(value));
    }

    private static void requireMoneyVisibility(String value, boolean canReadPrices) {
        if (canReadPrices != (value != null)) {
            throw new IllegalArgumentException("Order monetary visibility is inconsistent.");
        }
    }

    private static void requirePricingVisibility(
            OrderPricingSnapshotDto value,
            boolean canReadPrices) {
        if (canReadPrices != (value != null)) {
            throw new IllegalArgumentException("Order pricing visibility is inconsistent.");
        }
    }

    private static Optional<Instant> optionalInstant(String value) {
        return value == null ? Optional.empty() : Optional.of(Instant.parse(value));
    }

    private static <T extends Enum<T>> T enumValue(Class<T> type, String value) {
        return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
    }
}
