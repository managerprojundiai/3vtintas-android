package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;

public record MaterialQuoteUpdatePreviewResponse(
        String fingerprint,
        MaterialQuoteCustomerDto customer,
        Long organizationId,
        String title,
        String notes,
        String validUntil,
        String subtotal,
        String total,
        MaterialQuotePreviewPricingDto pricing,
        List<MaterialQuotePreviewLineDto> items,
        long quoteId,
        int expectedRevision,
        String discount) {
    private static final int MINIMUM_REVISION = 1;

    public MaterialQuoteUpdatePreviewResponse {
        new MaterialQuoteCreatePreviewResponse(
                fingerprint,
                customer,
                organizationId,
                title,
                notes,
                validUntil,
                subtotal,
                total,
                pricing,
                items);
        quoteId = DtoValidation.requirePositive(quoteId, "Preview quote ID");
        if (expectedRevision < MINIMUM_REVISION) {
            throw new IllegalArgumentException("Preview quote revision is invalid.");
        }
        discount = MaterialQuoteSummaryDto.money(discount);
        items = List.copyOf(items);
    }
}
