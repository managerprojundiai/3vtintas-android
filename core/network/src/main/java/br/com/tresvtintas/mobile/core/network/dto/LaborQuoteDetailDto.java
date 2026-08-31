package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;

public record LaborQuoteDetailDto(
        long id,
        LaborQuotePersonDto customer,
        LaborQuotePersonDto painter,
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
        List<LaborQuoteLineDto> items) {
    public LaborQuoteDetailDto {
        new LaborQuoteSummaryDto(
                id,
                customer,
                painter,
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
        notes = DtoValidation.optionalText(notes, "Labor quote notes", 4_000);
        if (items == null || items.isEmpty() || items.size() > 100 || items.contains(null)) {
            throw new IllegalArgumentException("Labor quote detail items are invalid.");
        }
        items = List.copyOf(items);
    }
}
