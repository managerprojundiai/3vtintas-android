package br.com.tresvtintas.mobile.core.finance;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record FinanceSnapshot(
        List<FinanceSummary> items,
        FinanceOverview overview,
        Optional<String> nextCursor) {
    public FinanceSnapshot {
        if (items == null
                || items.stream().anyMatch(java.util.Objects::isNull)
                || overview == null) {
            throw new IllegalArgumentException("Finance snapshot is invalid.");
        }
        items = List.copyOf(items);
        nextCursor = nextCursor == null ? Optional.empty() : nextCursor;
    }

    public static FinanceSnapshot from(FinancePage page) {
        return new FinanceSnapshot(
                page.items(),
                page.overview(),
                page.nextCursor());
    }

    public FinanceSnapshot append(FinancePage page) {
        List<FinanceSummary> combined = new ArrayList<>(items);
        combined.addAll(page.items());
        return new FinanceSnapshot(
                combined,
                page.overview(),
                page.nextCursor());
    }

    public boolean hasMore() {
        return nextCursor.isPresent();
    }
}
