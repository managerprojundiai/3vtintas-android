package br.com.tresvtintas.mobile.core.catalog;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

final class CatalogTestFixtures {
    private CatalogTestFixtures() {
    }

    static CatalogProduct product(long id, String name) {
        return new CatalogProduct(
                id,
                Optional.of(new CatalogCategory(7, "Tintas")),
                name,
                Optional.of("Tinta de acabamento premium"),
                Optional.empty(),
                Optional.of("SKU-" + id),
                Optional.of("UN"),
                Optional.of("18 L"),
                Optional.of(new BigDecimal("249.90")),
                Optional.of(12),
                Optional.of("3V"),
                Instant.parse("2026-07-25T12:00:00Z"));
    }

    static CatalogSnapshot snapshot(
            List<CatalogProduct> products,
            boolean hasMore,
            boolean stale) {
        return new CatalogSnapshot(
                products,
                hasMore,
                stale,
                CatalogSource.CACHE,
                Instant.parse("2026-07-25T12:00:00Z"));
    }
}
