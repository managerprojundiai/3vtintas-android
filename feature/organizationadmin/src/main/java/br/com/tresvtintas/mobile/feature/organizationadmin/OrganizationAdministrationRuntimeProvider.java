package br.com.tresvtintas.mobile.feature.organizationadmin;

import java.util.Optional;

@FunctionalInterface
public interface OrganizationAdministrationRuntimeProvider {
    Optional<OrganizationAdministrationFeatureRuntime>
            organizationAdministrationRuntime();
}
