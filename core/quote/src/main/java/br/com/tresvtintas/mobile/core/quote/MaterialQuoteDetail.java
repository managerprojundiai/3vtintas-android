package br.com.tresvtintas.mobile.core.quote;

import java.util.List;
import java.util.Optional;

public record MaterialQuoteDetail(
        MaterialQuoteSummary summary,
        Optional<String> notes,
        MaterialQuotePricingSnapshot pricing,
        List<MaterialQuoteLine> items) {
    public MaterialQuoteDetail {
        if (summary == null || pricing == null || items == null || items.isEmpty()
                || items.size() > 100) {
            throw new IllegalArgumentException("Quote detail is invalid.");
        }
        notes = notes == null ? Optional.empty() : notes;
        items = List.copyOf(items);
    }
}
