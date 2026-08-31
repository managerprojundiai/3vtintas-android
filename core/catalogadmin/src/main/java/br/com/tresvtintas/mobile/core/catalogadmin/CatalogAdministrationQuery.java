package br.com.tresvtintas.mobile.core.catalogadmin;

import java.util.Optional;
import java.util.OptionalLong;

public record CatalogAdministrationQuery(
        Optional<String> search,
        OptionalLong categoryId,
        Optional<Boolean> active,
        int pageSize) {
    public CatalogAdministrationQuery {
        search = search == null
                ? Optional.empty()
                : search.map(String::strip).filter(value -> !value.isEmpty());
        categoryId = categoryId == null ? OptionalLong.empty() : categoryId;
        active = active == null ? Optional.empty() : active;
        if (search.map(String::length).orElse(0) > 120
                || (categoryId.isPresent() && categoryId.getAsLong() < 1L)
                || pageSize < 1
                || pageSize > 100) {
            throw new IllegalArgumentException(
                    "Catalog administration query is invalid.");
        }
    }

    public static CatalogAdministrationQuery initial() {
        return new CatalogAdministrationQuery(
                Optional.empty(),
                OptionalLong.empty(),
                Optional.empty(),
                30);
    }

    public CatalogAdministrationQuery withSearch(String value) {
        return new CatalogAdministrationQuery(
                Optional.ofNullable(value),
                categoryId,
                active,
                pageSize);
    }

    public CatalogAdministrationQuery withActive(Optional<Boolean> value) {
        return new CatalogAdministrationQuery(
                search,
                categoryId,
                value,
                pageSize);
    }
}
