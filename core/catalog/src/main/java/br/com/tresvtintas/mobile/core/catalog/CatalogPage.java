package br.com.tresvtintas.mobile.core.catalog;

import java.util.List;
import java.util.Optional;

public record CatalogPage(List<CatalogProduct> items, Optional<String> nextCursor) {
    public CatalogPage {
        if (items == null || items.size() > 100 || items.stream().anyMatch(item -> item == null)) {
            throw new IllegalArgumentException("Catalog page items are invalid.");
        }
        items = List.copyOf(items);
        nextCursor = nextCursor == null ? Optional.empty() : nextCursor;
    }
}
