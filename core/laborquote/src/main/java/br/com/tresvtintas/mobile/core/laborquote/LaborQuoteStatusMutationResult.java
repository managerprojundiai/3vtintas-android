package br.com.tresvtintas.mobile.core.laborquote;

import java.math.BigDecimal;

public record LaborQuoteStatusMutationResult(
        long quoteId,
        LaborQuoteStatus previousStatus,
        LaborQuoteStatus status,
        int revision,
        BigDecimal total,
        boolean changed,
        boolean replayed) {
    public LaborQuoteStatusMutationResult {
        if (quoteId < 1
                || previousStatus == null
                || status == null
                || status == LaborQuoteStatus.CONVERTED
                || revision < 1
                || total == null
                || total.scale() != 2) {
            throw new IllegalArgumentException("Labor quote status result is invalid.");
        }
    }
}
