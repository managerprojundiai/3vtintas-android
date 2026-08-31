package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;

public record MaterialQuotePageDto(
        List<MaterialQuoteSummaryDto> items,
        String nextCursor) {
    public MaterialQuotePageDto {
        if (items == null || items.size() > 100 || items.contains(null)) {
            throw new IllegalArgumentException("Quote page is invalid.");
        }
        items = List.copyOf(items);
        nextCursor = DtoValidation.optionalText(
                nextCursor,
                "Quote cursor",
                256);
    }
}
