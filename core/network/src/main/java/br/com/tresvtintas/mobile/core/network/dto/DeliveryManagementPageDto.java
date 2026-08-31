package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;
import java.util.Objects;

public record DeliveryManagementPageDto(
        List<DeliveryManagementSummaryDto> items,
        String nextCursor) {
    public DeliveryManagementPageDto {
        if (items == null
                || items.size() > 100
                || items.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "Management delivery page is invalid.");
        }
        items = List.copyOf(items);
        nextCursor = DtoValidation.optionalText(
                nextCursor,
                "Management delivery cursor",
                160);
    }
}
