package br.com.tresvtintas.mobile.core.order;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record OrderSnapshot(List<OrderSummary> items, Optional<String> nextCursor) {
    public OrderSnapshot {
        if (items != null && items.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("Order snapshot is invalid.");
        }
        items = items == null ? List.of() : List.copyOf(items);
        nextCursor = nextCursor == null ? Optional.empty() : nextCursor;
    }

    public static OrderSnapshot from(OrderPage page) {
        return new OrderSnapshot(page.items(), page.nextCursor());
    }

    public OrderSnapshot append(OrderPage page) {
        List<OrderSummary> combined = new ArrayList<>(items);
        combined.addAll(page.items());
        return new OrderSnapshot(combined, page.nextCursor());
    }

    public boolean hasMore() { return nextCursor.isPresent(); }
}
