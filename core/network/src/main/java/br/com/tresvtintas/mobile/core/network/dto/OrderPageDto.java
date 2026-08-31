package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;

public record OrderPageDto(List<OrderSummaryDto> items, String nextCursor) {
    public OrderPageDto {
        if (items == null || items.size() > 100 || items.contains(null)) {
            throw new IllegalArgumentException("Order page is invalid.");
        }
        items = List.copyOf(items);
        nextCursor = DtoValidation.optionalText(nextCursor, "Order cursor", 96);
    }
}
