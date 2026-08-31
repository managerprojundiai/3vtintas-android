package br.com.tresvtintas.mobile.core.network.dto;

public record LaborQuoteMutationResponse(
        long quoteId,
        int revision,
        String total,
        boolean changed) {
    private static final int MINIMUM_REVISION = 1;

    public LaborQuoteMutationResponse {
        quoteId = DtoValidation.requirePositive(quoteId, "Labor quote ID");
        if (revision < MINIMUM_REVISION) {
            throw new IllegalArgumentException("Labor quote revision is invalid.");
        }
        total = LaborQuoteSummaryDto.money(total);
    }
}
