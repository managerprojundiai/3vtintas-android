package br.com.tresvtintas.mobile.core.catalog;

import java.util.Optional;

@FunctionalInterface
public interface CatalogPricingContextListener {
    void onCatalogPricingContextChanged(Optional<CatalogPricingContext> context);
}
