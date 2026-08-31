package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;
import java.util.regex.Pattern;

public record CatalogPageDto(
        List<CatalogProductDto> items,
        String nextCursor,
        CatalogPricingDto pricing) {
    private static final Pattern CURSOR = Pattern.compile("^[A-Za-z0-9_-]+$");

    public CatalogPageDto {
        if (items == null || items.size() > 100 || items.stream().anyMatch(item -> item == null)) {
            throw new IllegalArgumentException("Catalog page items are invalid.");
        }
        items = List.copyOf(items);
        if (nextCursor != null
                && (nextCursor.isBlank()
                || nextCursor.length() > 256
                || !CURSOR.matcher(nextCursor).matches())) {
            throw new IllegalArgumentException("Catalog page cursor is invalid.");
        }
    }
}
