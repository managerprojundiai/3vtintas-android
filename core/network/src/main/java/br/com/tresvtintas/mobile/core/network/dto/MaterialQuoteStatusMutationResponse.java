package br.com.tresvtintas.mobile.core.network.dto;

import java.util.Set;

public record MaterialQuoteStatusMutationResponse(
        long quoteId,
        String previousStatus,
        String status,
        int revision,
        String total,
        boolean pricingChanged,
        boolean changed) {
    private static final Set<String> STATUSES = Set.of(
            "draft", "sent", "accepted", "converted", "rejected", "expired");
    private static final Set<String> MANUAL_STATUSES = Set.of(
            "draft", "sent", "accepted", "rejected", "expired");

    public MaterialQuoteStatusMutationResponse {
        quoteId = DtoValidation.requirePositive(quoteId, "Quote ID");
        if (revision < 1
                || !STATUSES.contains(previousStatus)
                || !MANUAL_STATUSES.contains(status)) {
            throw new IllegalArgumentException(
                    "Quote status mutation response is invalid.");
        }
        total = MaterialQuoteSummaryDto.money(total);
    }
}
