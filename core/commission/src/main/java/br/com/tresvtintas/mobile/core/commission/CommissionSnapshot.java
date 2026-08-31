package br.com.tresvtintas.mobile.core.commission;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record CommissionSnapshot(
        List<CommissionSummary> items,
        CommissionOverview overview,
        Optional<String> nextCursor) {
    public CommissionSnapshot {
        if (items == null
                || items.stream().anyMatch(java.util.Objects::isNull)
                || overview == null) {
            throw new IllegalArgumentException("Commission snapshot is invalid.");
        }
        items = List.copyOf(items);
        nextCursor = nextCursor == null ? Optional.empty() : nextCursor;
    }

    public static CommissionSnapshot from(CommissionPage page) {
        return new CommissionSnapshot(
                page.items(),
                page.overview(),
                page.nextCursor());
    }

    public CommissionSnapshot append(CommissionPage page) {
        List<CommissionSummary> combined = new ArrayList<>(items);
        combined.addAll(page.items());
        return new CommissionSnapshot(
                combined,
                page.overview(),
                page.nextCursor());
    }

    public boolean hasMore() {
        return nextCursor.isPresent();
    }
}
