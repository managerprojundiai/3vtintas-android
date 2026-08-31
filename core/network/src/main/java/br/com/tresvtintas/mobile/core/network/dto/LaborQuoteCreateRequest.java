package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;

public record LaborQuoteCreateRequest(
        long customerId,
        String title,
        String notes,
        String validUntil,
        String discount,
        List<LaborQuoteLineRequest> items) {
    public LaborQuoteCreateRequest {
        customerId = DtoValidation.requirePositive(customerId, "Labor quote customer ID");
        title = DtoValidation.optionalText(title, "Labor quote title", 200);
        notes = DtoValidation.optionalText(notes, "Labor quote notes", 4_000);
        validUntil = DtoValidation.optionalText(
                validUntil,
                "Labor quote expiration",
                10);
        discount = LaborQuoteSummaryDto.inputMoney(discount);
        if (items == null || items.isEmpty() || items.size() > 100 || items.contains(null)) {
            throw new IllegalArgumentException("Labor quote request items are invalid.");
        }
        items = List.copyOf(items);
    }
}
