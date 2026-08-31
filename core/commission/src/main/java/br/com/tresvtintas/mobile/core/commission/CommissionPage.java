package br.com.tresvtintas.mobile.core.commission;

import java.util.List;
import java.util.Optional;

public record CommissionPage(
        List<CommissionSummary> items,
        CommissionOverview overview,
        Optional<String> nextCursor) {
    public CommissionPage {
        if (items == null
                || items.size() > 100
                || items.stream().anyMatch(java.util.Objects::isNull)
                || overview == null) {
            throw new IllegalArgumentException("Commission page is invalid.");
        }
        items = List.copyOf(items);
        nextCursor = nextCursor == null ? Optional.empty() : nextCursor;
    }
}
