package br.com.tresvtintas.mobile.core.corporatefinance;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record CorporateFinanceOrganizationSnapshot(
        List<CorporateFinanceOrganization> items,
        Optional<String> nextCursor) {
    public CorporateFinanceOrganizationSnapshot {
        if (items == null
                || items.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException(
                    "Corporate finance organization snapshot is invalid.");
        }
        items = List.copyOf(items);
        nextCursor = nextCursor == null ? Optional.empty() : nextCursor;
    }

    public static CorporateFinanceOrganizationSnapshot from(
            CorporateFinanceOrganizationPage page) {
        return new CorporateFinanceOrganizationSnapshot(
                page.items(),
                page.nextCursor());
    }

    public CorporateFinanceOrganizationSnapshot append(
            CorporateFinanceOrganizationPage page) {
        List<CorporateFinanceOrganization> combined =
                new ArrayList<>(items);
        combined.addAll(page.items());
        return new CorporateFinanceOrganizationSnapshot(
                combined,
                page.nextCursor());
    }

    public boolean hasMore() {
        return nextCursor.isPresent();
    }
}
