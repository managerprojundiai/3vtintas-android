package br.com.tresvtintas.mobile.core.catalogadmin;

import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationModels.Product;
import java.util.List;
import java.util.Optional;

public record CatalogAdministrationListState(
        Phase phase,
        List<Product> products,
        Optional<String> nextCursor,
        Optional<CatalogAdministrationException> failure,
        boolean loadingMore) {
    public enum Phase {
        EMPTY,
        LOADING,
        READY,
        STALE,
        ERROR,
        CLOSED
    }

    public CatalogAdministrationListState {
        products = products == null ? List.of() : List.copyOf(products);
        nextCursor = nextCursor == null ? Optional.empty() : nextCursor;
        failure = failure == null ? Optional.empty() : failure;
    }

    @Override
    public List<Product> products() {
        return List.copyOf(products);
    }

    public static CatalogAdministrationListState empty() {
        return new CatalogAdministrationListState(
                Phase.EMPTY, List.of(), Optional.empty(), Optional.empty(), false);
    }
}
