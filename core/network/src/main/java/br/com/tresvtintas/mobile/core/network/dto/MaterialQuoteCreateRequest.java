package br.com.tresvtintas.mobile.core.network.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MaterialQuoteCreateRequest(
        long customerId,
        String title,
        String notes,
        String validUntil,
        MaterialQuotePricingSelectionRequest pricing,
        List<MaterialQuoteLineRequest> items,
        String expectedPreviewFingerprint) {
    public MaterialQuoteCreateRequest {
        customerId = DtoValidation.requirePositive(customerId, "Quote customer ID");
        title = DtoValidation.optionalText(title, "Quote title", 200);
        notes = DtoValidation.optionalText(notes, "Quote notes", 4_000);
        validUntil = DtoValidation.optionalText(validUntil, "Quote expiration", 10);
        if (items == null || items.isEmpty() || items.size() > 100 || items.contains(null)) {
            throw new IllegalArgumentException("Quote request items are invalid.");
        }
        items = List.copyOf(items);
        expectedPreviewFingerprint = DtoValidation.requireSha256(
                expectedPreviewFingerprint,
                "Quote preview fingerprint");
    }
}
