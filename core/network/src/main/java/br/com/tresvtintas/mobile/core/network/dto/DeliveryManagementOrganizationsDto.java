package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;
import java.util.Objects;

public record DeliveryManagementOrganizationsDto(
        List<DeliveryManagementOrganizationDto> items) {
    public DeliveryManagementOrganizationsDto {
        if (items == null
                || items.size() > 1_000
                || items.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "Management organizations are invalid.");
        }
        items = List.copyOf(items);
    }
}
