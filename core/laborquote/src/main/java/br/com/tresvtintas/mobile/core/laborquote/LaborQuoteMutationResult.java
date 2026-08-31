package br.com.tresvtintas.mobile.core.laborquote;

import java.math.BigDecimal;

public record LaborQuoteMutationResult(
        long quoteId,
        int revision,
        BigDecimal total,
        boolean changed,
        boolean replayed) {
    public LaborQuoteMutationResult {
        if (quoteId < 1 || revision < 1 || total == null || total.scale() != 2) {
            throw new IllegalArgumentException("Labor quote mutation result is invalid.");
        }
    }
}
