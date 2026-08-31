package br.com.tresvtintas.mobile.core.laborquote;

import java.util.List;
import java.util.Optional;

public record LaborQuoteDetail(
        LaborQuoteSummary summary,
        Optional<String> notes,
        List<LaborQuoteLine> items) {
    public LaborQuoteDetail {
        if (summary == null || items == null || items.isEmpty() || items.size() > 100) {
            throw new IllegalArgumentException("Labor quote detail is invalid.");
        }
        notes = notes == null ? Optional.empty() : notes;
        items = List.copyOf(items);
    }
}
