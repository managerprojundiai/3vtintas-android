package br.com.tresvtintas.mobile.core.delivery;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record DeliveryManagementPage(
        List<DeliveryManagementSummary> items,
        Optional<String> nextCursor) {
    public DeliveryManagementPage {
        if (items == null
                || items.size() > 100
                || items.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "Delivery management page is invalid.");
        }
        items = List.copyOf(items);
        nextCursor = Objects.requireNonNull(
                nextCursor,
                "Delivery management cursor is required.");
    }
}
