package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;

public record LaborQuotePageDto(List<LaborQuoteSummaryDto> items, String nextCursor) {
    public LaborQuotePageDto {
        if (items == null || items.size() > 100 || items.contains(null)) {
            throw new IllegalArgumentException("Labor quote page is invalid.");
        }
        items = List.copyOf(items);
        nextCursor = DtoValidation.optionalText(
                nextCursor,
                "Labor quote cursor",
                256);
    }
}
