package br.com.tresvtintas.mobile.core.catalog;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record CatalogSnapshot(
        List<CatalogProduct> items,
        boolean hasMore,
        boolean stale,
        CatalogSource source,
        Instant refreshedAt) {
    public CatalogSnapshot {
        items = List.copyOf(Objects.requireNonNull(items, "Catalog items are required."));
        if (items.stream().anyMatch(item -> item == null)) {
            throw new IllegalArgumentException("Catalog items cannot contain null.");
        }
        Objects.requireNonNull(source, "Catalog source is required.");
        Objects.requireNonNull(refreshedAt, "Catalog refresh instant is required.");
        if (source == CatalogSource.NETWORK && stale) {
            throw new IllegalArgumentException("A network snapshot cannot start stale.");
        }
    }

    public CatalogSnapshot asStale() {
        return new CatalogSnapshot(
                items,
                hasMore,
                true,
                source,
                refreshedAt);
    }
}
