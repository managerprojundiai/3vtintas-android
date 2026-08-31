package br.com.tresvtintas.mobile.core.corporatefinance;

import br.com.tresvtintas.mobile.core.finance.FinanceException;
import java.util.Optional;

@FunctionalInterface
public interface CorporateFinanceOrganizationRepository {
    CorporateFinanceOrganizationPage page(
            Optional<String> search,
            Optional<String> cursor,
            int pageSize) throws FinanceException;
}
