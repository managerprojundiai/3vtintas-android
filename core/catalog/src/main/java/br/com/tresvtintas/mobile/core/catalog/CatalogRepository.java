package br.com.tresvtintas.mobile.core.catalog;

import java.util.Optional;

public interface CatalogRepository {
    default Optional<CatalogPricingContext> pricingContext() throws CatalogException {
        return Optional.empty();
    }

    default void selectPriceList(String versionPublicId) throws CatalogException {
        throw new CatalogException(
                CatalogFailureKind.FORBIDDEN,
                "Catalog table selection is not available.");
    }

    default boolean allowsStalePrices() {
        return true;
    }

    Optional<CatalogSnapshot> cached(CatalogQuery query) throws CatalogException;

    CatalogSnapshot refresh(CatalogQuery query) throws CatalogException;

    CatalogSnapshot loadMore(CatalogQuery query) throws CatalogException;

    void clearAccount() throws CatalogException;
}
