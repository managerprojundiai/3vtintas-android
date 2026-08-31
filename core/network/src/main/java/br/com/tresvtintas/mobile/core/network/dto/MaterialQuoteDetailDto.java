package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;

public record MaterialQuoteDetailDto(
        long id,
        MaterialQuoteCustomerDto customer,
        Long organizationId,
        String title,
        String status,
        String subtotal,
        String discount,
        String total,
        int revision,
        int itemCount,
        String validUntil,
        String createdAt,
        String updatedAt,
        String notes,
        MaterialQuotePricingSnapshotDto pricing,
        List<MaterialQuoteLineDto> items) {
    public MaterialQuoteDetailDto {
        new MaterialQuoteSummaryDto(
                id,
                customer,
                organizationId,
                title,
                status,
                subtotal,
                discount,
                total,
                revision,
                itemCount,
                validUntil,
                createdAt,
                updatedAt);
        notes = DtoValidation.optionalText(notes, "Quote notes", 4_000);
        if (pricing == null) {
            throw new IllegalArgumentException("Quote pricing snapshot is required.");
        }
        if (items == null || items.isEmpty() || items.size() > 100 || items.contains(null)) {
            throw new IllegalArgumentException("Quote detail items are invalid.");
        }
        items = List.copyOf(items);
    }
}
