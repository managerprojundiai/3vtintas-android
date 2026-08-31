package br.com.tresvtintas.mobile.core.quote;

import java.math.BigDecimal;

public record MaterialQuoteMutationResult(
        long quoteId,
        int revision,
        BigDecimal total,
        boolean changed,
        boolean replayed) {
    public MaterialQuoteMutationResult {
        if (quoteId < 1 || revision < 1 || total == null || total.scale() != 2) {
            throw new IllegalArgumentException("Quote mutation result is invalid.");
        }
    }
}
