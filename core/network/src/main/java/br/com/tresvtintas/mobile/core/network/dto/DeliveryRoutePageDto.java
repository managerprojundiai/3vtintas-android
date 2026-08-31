package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;

public record DeliveryRoutePageDto(List<DeliveryRoutePlanDto> items) {
    public DeliveryRoutePageDto {
        if (items == null || items.size() > 100
                || items.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("Delivery route page is invalid.");
        }
        items = List.copyOf(items);
    }
}
