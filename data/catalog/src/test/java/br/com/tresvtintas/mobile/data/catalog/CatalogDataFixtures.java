package br.com.tresvtintas.mobile.data.catalog;

import br.com.tresvtintas.mobile.core.catalog.CatalogCategory;
import br.com.tresvtintas.mobile.core.catalog.CatalogPage;
import br.com.tresvtintas.mobile.core.catalog.CatalogProduct;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

final class CatalogDataFixtures {
    private CatalogDataFixtures() {
    }

    static CatalogAccountScope scope() {
        return CatalogAccountScope.from(42, "a".repeat(64), 9);
    }

    static CatalogProduct product(long id) {
        return new CatalogProduct(
                id,
                Optional.of(new CatalogCategory(3, "Esmaltes")),
                "Produto " + id,
                Optional.of("Descrição"),
                Optional.empty(),
                Optional.of("SKU-" + id),
                Optional.of("UN"),
                Optional.of("3,6 L"),
                Optional.of(new BigDecimal("119.90")),
                Optional.of(9),
                Optional.of("Marca"),
                Instant.parse("2026-07-25T12:00:00Z"));
    }

    static CatalogPage page(List<CatalogProduct> products, String nextCursor) {
        return new CatalogPage(products, Optional.ofNullable(nextCursor));
    }
}
