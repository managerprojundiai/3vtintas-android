package br.com.tresvtintas.mobile.core.customer;

import java.util.List;
import java.util.Optional;

public record CustomerPage(
        List<CustomerSummary> items,
        Optional<String> nextCursor) {
    private static final int MAXIMUM_PAGE_SIZE = 100;

    public CustomerPage {
        items = items == null ? List.of() : List.copyOf(items);
        if (items.size() > MAXIMUM_PAGE_SIZE) {
            throw new IllegalArgumentException("Customer page is too large.");
        }
        nextCursor = nextCursor == null ? Optional.empty() : nextCursor;
    }
}
