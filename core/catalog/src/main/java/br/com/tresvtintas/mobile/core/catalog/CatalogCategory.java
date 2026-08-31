package br.com.tresvtintas.mobile.core.catalog;

public record CatalogCategory(long id, String name) {
    private static final int MINIMUM_IDENTIFIER = 1;

    public CatalogCategory {
        if (id < MINIMUM_IDENTIFIER) {
            throw new IllegalArgumentException("Catalog category ID must be positive.");
        }
        if (name == null || name.isBlank() || name.length() > 100) {
            throw new IllegalArgumentException("Catalog category name is invalid.");
        }
    }
}
