package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;

public record MaterialQuoteCreatePreviewResponse(
        String fingerprint,
        MaterialQuoteCustomerDto customer,
        Long organizationId,
        String title,
        String notes,
        String validUntil,
        String subtotal,
        String total,
        MaterialQuotePreviewPricingDto pricing,
        List<MaterialQuotePreviewLineDto> items) {
    public MaterialQuoteCreatePreviewResponse {
        fingerprint = DtoValidation.requireSha256(
                fingerprint, "Quote preview fingerprint");
        if (customer == null || pricing == null || items == null
                || items.isEmpty() || items.size() > 100 || items.contains(null)) {
            throw new IllegalArgumentException("Quote preview response is invalid.");
        }
        organizationId = DtoValidation.optionalPositive(
                organizationId, "Preview organization ID");
        title = DtoValidation.requireText(title, "Preview title", 200);
        notes = DtoValidation.optionalText(notes, "Preview notes", 4_000);
        if (validUntil != null) {
            validUntil = DtoValidation.requireInstant(
                    validUntil, "Preview expiration");
        }
        subtotal = MaterialQuoteSummaryDto.money(subtotal);
        total = MaterialQuoteSummaryDto.money(total);
        items = List.copyOf(items);
    }
}
