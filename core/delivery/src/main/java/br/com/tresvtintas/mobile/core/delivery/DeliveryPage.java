package br.com.tresvtintas.mobile.core.delivery;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record DeliveryPage(
        List<DeliverySummary> items,
        Optional<String> nextCursor) {
    public DeliveryPage {
        if (items == null
                || items.stream().anyMatch(Objects::isNull)
                || items.size() > 100) {
            throw new IllegalArgumentException("Delivery page is invalid.");
        }
        items = List.copyOf(items);
        nextCursor = Objects.requireNonNull(
                nextCursor,
                "Delivery cursor is required.");
    }
}
