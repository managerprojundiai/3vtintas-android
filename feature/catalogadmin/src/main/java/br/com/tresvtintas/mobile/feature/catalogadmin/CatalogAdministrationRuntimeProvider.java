package br.com.tresvtintas.mobile.feature.catalogadmin;

import java.util.Optional;

@FunctionalInterface
public interface CatalogAdministrationRuntimeProvider {
    Optional<CatalogAdministrationFeatureRuntime>
            catalogAdministrationRuntime();
}
