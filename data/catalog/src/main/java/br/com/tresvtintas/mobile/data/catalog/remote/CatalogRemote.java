package br.com.tresvtintas.mobile.data.catalog.remote;

import br.com.tresvtintas.mobile.core.catalog.CatalogException;
import br.com.tresvtintas.mobile.core.catalog.CatalogPage;
import br.com.tresvtintas.mobile.core.catalog.CatalogQuery;
import br.com.tresvtintas.mobile.core.catalog.CatalogPricingContext;
import java.util.Optional;

public interface CatalogRemote {
    Optional<CatalogPricingContext> pricingContext() throws CatalogException;

    void selectPriceList(String versionPublicId) throws CatalogException;

    boolean canReadPrices();

    CatalogPage fetch(CatalogQuery query, Optional<String> cursor)
            throws CatalogException;
}
