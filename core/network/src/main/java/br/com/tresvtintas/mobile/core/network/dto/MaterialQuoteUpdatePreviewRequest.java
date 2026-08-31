package br.com.tresvtintas.mobile.core.network.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MaterialQuoteUpdatePreviewRequest(
        int expectedRevision,
        long customerId,
        String title,
        String notes,
        String validUntil,
        MaterialQuotePricingSelectionRequest pricing,
        List<MaterialQuoteLineRequest> items) {
    private static final int MINIMUM_REVISION = 1;

    public MaterialQuoteUpdatePreviewRequest {
        if (expectedRevision < MINIMUM_REVISION) {
            throw new IllegalArgumentException("Quote revision is invalid.");
        }
        new MaterialQuoteCreatePreviewRequest(
                customerId,
                title,
                notes,
                validUntil,
                pricing,
                items);
        items = List.copyOf(items);
    }
}
