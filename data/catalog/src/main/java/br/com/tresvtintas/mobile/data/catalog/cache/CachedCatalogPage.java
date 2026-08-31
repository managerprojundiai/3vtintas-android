package br.com.tresvtintas.mobile.data.catalog.cache;

import br.com.tresvtintas.mobile.core.catalog.CatalogProduct;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public record CachedCatalogPage(
        List<CatalogProduct> items,
        Optional<String> nextCursor,
        Instant refreshedAt) {
    public CachedCatalogPage {
        items = List.copyOf(items);
        nextCursor = nextCursor == null ? Optional.empty() : nextCursor;
    }
}
