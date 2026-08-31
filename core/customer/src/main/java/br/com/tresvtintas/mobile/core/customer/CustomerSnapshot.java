package br.com.tresvtintas.mobile.core.customer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record CustomerSnapshot(
        List<CustomerSummary> items,
        Optional<String> nextCursor) {
    public CustomerSnapshot {
        items = items == null ? List.of() : List.copyOf(items);
        nextCursor = nextCursor == null ? Optional.empty() : nextCursor;
    }

    public static CustomerSnapshot from(CustomerPage page) {
        return new CustomerSnapshot(page.items(), page.nextCursor());
    }

    public CustomerSnapshot append(CustomerPage page) {
        Map<Long, CustomerSummary> merged = new LinkedHashMap<>();
        for (CustomerSummary item : items) {
            merged.put(item.id(), item);
        }
        for (CustomerSummary item : page.items()) {
            merged.put(item.id(), item);
        }
        return new CustomerSnapshot(
                List.copyOf(merged.values()),
                page.nextCursor());
    }

    public boolean hasMore() {
        return nextCursor.isPresent();
    }
}
