package br.com.tresvtintas.mobile.core.network.dto;

public record LaborQuoteStatusMutationResponse(
        long quoteId,
        String previousStatus,
        String status,
        int revision,
        String total,
        boolean changed) {
    public LaborQuoteStatusMutationResponse {
        quoteId = DtoValidation.requirePositive(quoteId, "Labor quote ID");
        if (revision < 1
                || !LaborQuoteValidation.STATUSES.contains(previousStatus)
                || !LaborQuoteValidation.MANUAL_STATUSES.contains(status)) {
            throw new IllegalArgumentException("Labor quote status response is invalid.");
        }
        total = LaborQuoteSummaryDto.money(total);
    }
}
