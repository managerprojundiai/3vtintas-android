package br.com.tresvtintas.mobile.data.catalog.remote;

import br.com.tresvtintas.mobile.core.catalog.CatalogCategory;
import br.com.tresvtintas.mobile.core.catalog.CatalogPage;
import br.com.tresvtintas.mobile.core.catalog.CatalogProduct;
import br.com.tresvtintas.mobile.core.network.dto.CatalogCategoryDto;
import br.com.tresvtintas.mobile.core.network.dto.CatalogPageDto;
import br.com.tresvtintas.mobile.core.network.dto.CatalogProductDto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Collectors;

final class CatalogDtoMapper {
    private CatalogDtoMapper() {
        throw new AssertionError("No instances.");
    }

    static CatalogPage toDomain(CatalogPageDto page) {
        return new CatalogPage(
                page.items().stream()
                        .map(CatalogDtoMapper::toDomain)
                        .collect(Collectors.toList()),
                Optional.ofNullable(page.nextCursor()));
    }

    private static CatalogProduct toDomain(CatalogProductDto product) {
        return new CatalogProduct(
                product.id(),
                Optional.ofNullable(product.category()).map(CatalogDtoMapper::toDomain),
                product.name(),
                Optional.ofNullable(product.description()),
                Optional.ofNullable(product.imageUrl()),
                Optional.ofNullable(product.sku()),
                Optional.ofNullable(product.unit()),
                Optional.ofNullable(product.volume()),
                Optional.ofNullable(product.price()).map(BigDecimal::new),
                Optional.ofNullable(product.stock()),
                Optional.ofNullable(product.brand()),
                Instant.parse(product.updatedAt()));
    }

    private static CatalogCategory toDomain(CatalogCategoryDto category) {
        return new CatalogCategory(category.id(), category.name());
    }
}
