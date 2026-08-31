package br.com.tresvtintas.mobile.core.network.dto;

import java.util.regex.Pattern;

public record CatalogProductDto(
        long id,
        CatalogCategoryDto category,
        String name,
        String description,
        String imageUrl,
        String sku,
        String unit,
        String volume,
        String price,
        Integer stock,
        String brand,
        String updatedAt) {
    private static final Pattern PRICE = Pattern.compile("^\\d{1,8}\\.\\d{2}$");
    private static final int MINIMUM_IDENTIFIER = 1;

    public CatalogProductDto {
        if (id < MINIMUM_IDENTIFIER) {
            throw new IllegalArgumentException("Catalog product ID must be positive.");
        }
        name = DtoValidation.requireText(name, "Catalog product name", 200);
        description = optionalText(description, "Catalog product description", 4_000);
        imageUrl = optionalText(imageUrl, "Catalog product image URL", 2_048);
        sku = optionalText(sku, "Catalog product SKU", 50);
        unit = optionalText(unit, "Catalog product unit", 20);
        volume = optionalText(volume, "Catalog product volume", 20);
        brand = optionalText(brand, "Catalog product brand", 100);
        if (price != null
                && (!PRICE.matcher(price).matches()
                || "0.00".equals(price))) {
            throw new IllegalArgumentException("Catalog product price is invalid.");
        }
        updatedAt = DtoValidation.requireInstant(
                updatedAt, "Catalog product update instant");
    }

    private static String optionalText(
            String value,
            String fieldName,
            int maximumLength) {
        if (value != null && value.length() > maximumLength) {
            throw new IllegalArgumentException(fieldName + " is too long.");
        }
        return value;
    }
}
