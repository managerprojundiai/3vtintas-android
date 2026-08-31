package br.com.tresvtintas.mobile.core.order;

import java.util.List;
import java.util.Optional;

public record OrderPage(List<OrderSummary> items, Optional<String> nextCursor) {
    private static final int MAX_ITEMS_PER_PAGE = 100;

    public OrderPage {
        if (items != null && items.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("Order page is invalid.");
        }
        items = items == null ? List.of() : List.copyOf(items);
        nextCursor = nextCursor == null ? Optional.empty() : nextCursor;
        if (items.size() > MAX_ITEMS_PER_PAGE) {
            throw new IllegalArgumentException("Order page is invalid.");
        }
    }
}
