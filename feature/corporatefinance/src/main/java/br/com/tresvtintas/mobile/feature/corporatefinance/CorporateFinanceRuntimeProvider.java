package br.com.tresvtintas.mobile.feature.corporatefinance;

import java.util.Optional;

@FunctionalInterface
public interface CorporateFinanceRuntimeProvider {
    Optional<CorporateFinanceFeatureRuntime> corporateFinanceRuntime();
}
