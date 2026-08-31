package br.com.tresvtintas.mobile.core.quote;

import java.math.BigDecimal;

public record MaterialQuoteStatusMutationResult(
        long quoteId,
        MaterialQuoteStatus previousStatus,
        MaterialQuoteStatus status,
        int revision,
        BigDecimal total,
        boolean pricingChanged,
        boolean changed,
        boolean replayed) {
    public MaterialQuoteStatusMutationResult {
        if (quoteId < 1
                || previousStatus == null
                || status == null
                || status == MaterialQuoteStatus.CONVERTED
                || revision < 1
                || total == null
                || total.scale() != 2) {
            throw new IllegalArgumentException(
                    "Quote status mutation result is invalid.");
        }
    }
}
