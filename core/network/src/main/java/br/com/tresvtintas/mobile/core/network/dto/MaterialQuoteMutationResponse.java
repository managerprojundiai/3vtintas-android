package br.com.tresvtintas.mobile.core.network.dto;

public record MaterialQuoteMutationResponse(
        long quoteId,
        int revision,
        String total,
        boolean changed) {
    private static final int MINIMUM_REVISION = 1;

    public MaterialQuoteMutationResponse {
        quoteId = DtoValidation.requirePositive(quoteId, "Quote ID");
        if (revision < MINIMUM_REVISION) {
            throw new IllegalArgumentException("Quote revision is invalid.");
        }
        total = MaterialQuoteSummaryDto.money(total);
    }
}
