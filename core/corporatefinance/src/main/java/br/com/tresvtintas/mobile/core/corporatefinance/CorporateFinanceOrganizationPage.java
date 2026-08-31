package br.com.tresvtintas.mobile.core.corporatefinance;

import java.util.List;
import java.util.Optional;

public record CorporateFinanceOrganizationPage(
        List<CorporateFinanceOrganization> items,
        Optional<String> nextCursor) {
    public CorporateFinanceOrganizationPage {
        if (items == null
                || items.size() > 100
                || items.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException(
                    "Corporate finance organization page is invalid.");
        }
        items = List.copyOf(items);
        nextCursor = nextCursor == null ? Optional.empty() : nextCursor;
    }
}
