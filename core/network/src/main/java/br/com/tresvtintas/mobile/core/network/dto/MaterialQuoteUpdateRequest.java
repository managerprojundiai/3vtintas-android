package br.com.tresvtintas.mobile.core.network.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MaterialQuoteUpdateRequest(
        int expectedRevision,
        long customerId,
        String title,
        String notes,
        String validUntil,
        MaterialQuotePricingSelectionRequest pricing,
        List<MaterialQuoteLineRequest> items,
        String expectedPreviewFingerprint) {
    private static final int MINIMUM_REVISION = 1;

    public MaterialQuoteUpdateRequest {
        if (expectedRevision < MINIMUM_REVISION) {
            throw new IllegalArgumentException("Quote revision is invalid.");
        }
        new MaterialQuoteCreateRequest(
                customerId,
                title,
                notes,
                validUntil,
                pricing,
                items,
                expectedPreviewFingerprint);
        items = List.copyOf(items);
    }
}
