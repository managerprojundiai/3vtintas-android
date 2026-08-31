package br.com.tresvtintas.mobile.feature.laborquote;

import br.com.tresvtintas.mobile.core.customer.CustomerSummary;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteDraftLine;
import java.util.List;

record LaborQuoteEditorSnapshot(
        CustomerSummary customer,
        List<LaborQuoteDraftLine> lines,
        String title,
        String notes,
        String discount,
        int revision,
        String idempotencyKey,
        String fingerprint) {
    LaborQuoteEditorSnapshot {
        lines = List.copyOf(lines);
    }
}
