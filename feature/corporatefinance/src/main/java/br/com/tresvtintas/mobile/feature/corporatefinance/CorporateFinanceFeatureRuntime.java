package br.com.tresvtintas.mobile.feature.corporatefinance;

import br.com.tresvtintas.mobile.core.corporatefinance.CorporateFinanceOrganizationRepository;
import java.util.Objects;
import java.util.concurrent.Executor;

public record CorporateFinanceFeatureRuntime(
        CorporateFinanceOrganizationRepository organizationRepository,
        Executor workerExecutor,
        boolean globalAccess) {
    public CorporateFinanceFeatureRuntime {
        Objects.requireNonNull(
                organizationRepository,
                "Corporate finance organization repository is required.");
        Objects.requireNonNull(
                workerExecutor,
                "Corporate finance worker is required.");
    }
}
