package br.com.tresvtintas.mobile.core.network.dto;

public record CatalogCategoryDto(long id, String name) {
    private static final int MINIMUM_IDENTIFIER = 1;

    public CatalogCategoryDto {
        if (id < MINIMUM_IDENTIFIER) {
            throw new IllegalArgumentException("Catalog category ID must be positive.");
        }
        name = DtoValidation.requireText(name, "Catalog category name", 100);
    }
}
