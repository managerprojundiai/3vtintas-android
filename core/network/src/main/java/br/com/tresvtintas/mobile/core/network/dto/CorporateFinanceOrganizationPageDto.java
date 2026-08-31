package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;

public record CorporateFinanceOrganizationPageDto(
        List<CorporateFinanceOrganizationDto> items,
        String nextCursor) {
    public CorporateFinanceOrganizationPageDto {
        if (items == null
                || items.size() > 100
                || items.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException(
                    "Corporate finance organization page is invalid.");
        }
        items = List.copyOf(items);
        nextCursor = DtoValidation.optionalText(
                nextCursor,
                "Corporate finance organization cursor",
                128);
    }
}
