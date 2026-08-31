package br.com.tresvtintas.mobile.feature.catalog;

import br.com.tresvtintas.mobile.core.catalog.CatalogCategory;
import java.util.OptionalLong;

record CatalogCategoryOption(OptionalLong id, String label) {
    CatalogCategoryOption {
        id = id == null ? OptionalLong.empty() : id;
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Catalog category label is required.");
        }
    }

    static CatalogCategoryOption all(String label) {
        return new CatalogCategoryOption(OptionalLong.empty(), label);
    }

    static CatalogCategoryOption from(CatalogCategory category) {
        return new CatalogCategoryOption(
                OptionalLong.of(category.id()),
                category.name());
    }
}
