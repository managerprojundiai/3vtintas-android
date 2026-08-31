package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;

public record DeliveryPageDto(
        List<DeliverySummaryDto> items,
        String nextCursor) {
    public DeliveryPageDto {
        if (items == null || items.size() > 100 || items.contains(null)) {
            throw new IllegalArgumentException("Delivery page is invalid.");
        }
        items = List.copyOf(items);
        nextCursor = DtoValidation.optionalText(
                nextCursor,
                "Delivery cursor",
                128);
    }
}
