package br.com.tresvtintas.mobile.core.finance;

import java.util.List;
import java.util.Optional;

public record FinancePage(
        List<FinanceSummary> items,
        FinanceOverview overview,
        Optional<String> nextCursor) {
    public FinancePage {
        if (items == null
                || items.size() > 100
                || items.stream().anyMatch(java.util.Objects::isNull)
                || overview == null) {
            throw new IllegalArgumentException("Finance page is invalid.");
        }
        items = List.copyOf(items);
        nextCursor = nextCursor == null ? Optional.empty() : nextCursor;
    }
}
