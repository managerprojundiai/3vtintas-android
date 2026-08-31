package br.com.tresvtintas.mobile.core.corporatefinance;

@FunctionalInterface
public interface CorporateFinanceOrganizationStateListener {
    void onCorporateFinanceOrganizationStateChanged(
            CorporateFinanceOrganizationState state);
}
