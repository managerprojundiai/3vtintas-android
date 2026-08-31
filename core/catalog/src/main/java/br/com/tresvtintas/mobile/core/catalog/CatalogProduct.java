package br.com.tresvtintas.mobile.core.catalog;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record CatalogProduct(
        long id,
        Optional<CatalogCategory> category,
        String name,
        Optional<String> description,
        Optional<String> imageUrl,
        Optional<String> sku,
        Optional<String> unit,
        Optional<String> volume,
        Optional<BigDecimal> price,
        Optional<Integer> stock,
        Optional<String> brand,
        Instant updatedAt) {
    private static final int MINIMUM_IDENTIFIER = 1;

    public CatalogProduct {
        if (id < MINIMUM_IDENTIFIER) {
            throw new IllegalArgumentException("Catalog product ID must be positive.");
        }
        category = optional(category);
        if (name == null || name.isBlank() || name.length() > 200) {
            throw new IllegalArgumentException("Catalog product name is invalid.");
        }
        description = optional(description);
        imageUrl = optional(imageUrl);
        sku = optional(sku);
        unit = optional(unit);
        volume = optional(volume);
        brand = optional(brand);
        price = optional(price);
        if (price.isPresent()
                && (price.get().scale() != 2 || price.get().signum() <= 0)) {
            throw new IllegalArgumentException("Catalog product price is invalid.");
        }
        stock = optional(stock);
        Objects.requireNonNull(updatedAt, "Catalog product update instant is required.");
    }

    private static <T> Optional<T> optional(Optional<T> value) {
        return value == null ? Optional.empty() : value;
    }
}
