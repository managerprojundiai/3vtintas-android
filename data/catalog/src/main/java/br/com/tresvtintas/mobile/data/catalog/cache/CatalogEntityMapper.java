package br.com.tresvtintas.mobile.data.catalog.cache;

import br.com.tresvtintas.mobile.core.catalog.CatalogCategory;
import br.com.tresvtintas.mobile.core.catalog.CatalogProduct;
import br.com.tresvtintas.mobile.data.catalog.CatalogAccountScope;
import br.com.tresvtintas.mobile.data.catalog.db.CatalogProductEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

final class CatalogEntityMapper {
    private CatalogEntityMapper() {
        throw new AssertionError("No instances.");
    }

    static CatalogProductEntity toEntity(
            CatalogAccountScope scope,
            CatalogProduct product) {
        Long categoryId = product.category().map(CatalogCategory::id).orElse(null);
        String categoryName = product.category().map(CatalogCategory::name).orElse(null);
        return new CatalogProductEntity(
                scope.accountKey(),
                scope.authorizationRevision(),
                product.id(),
                categoryId,
                categoryName,
                product.name(),
                product.description().orElse(null),
                product.imageUrl().orElse(null),
                product.sku().orElse(null),
                product.unit().orElse(null),
                product.volume().orElse(null),
                product.price().map(BigDecimal::toPlainString).orElse(null),
                product.stock().orElse(null),
                product.brand().orElse(null),
                product.updatedAt().toEpochMilli());
    }

    static CatalogProduct toDomain(CatalogProductEntity entity) {
        Optional<CatalogCategory> category = entity.categoryId == null
                ? Optional.empty()
                : Optional.of(new CatalogCategory(
                        entity.categoryId,
                        entity.categoryName));
        return new CatalogProduct(
                entity.productId,
                category,
                entity.name,
                Optional.ofNullable(entity.description),
                Optional.ofNullable(entity.imageUrl),
                Optional.ofNullable(entity.sku),
                Optional.ofNullable(entity.unit),
                Optional.ofNullable(entity.volume),
                Optional.ofNullable(entity.price).map(BigDecimal::new),
                Optional.ofNullable(entity.stock),
                Optional.ofNullable(entity.brand),
                Instant.ofEpochMilli(entity.updatedAtMillis));
    }
}
