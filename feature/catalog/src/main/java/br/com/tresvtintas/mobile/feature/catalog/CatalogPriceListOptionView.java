package br.com.tresvtintas.mobile.feature.catalog;

import br.com.tresvtintas.mobile.core.catalog.CatalogPriceListOption;

record CatalogPriceListOptionView(String versionPublicId, String label) {
    static CatalogPriceListOptionView from(CatalogPriceListOption option) {
        String primary = option.primary() ? " · principal" : "";
        return new CatalogPriceListOptionView(
                option.versionPublicId(),
                option.name() + " · versão " + option.versionNumber() + primary);
    }
}
