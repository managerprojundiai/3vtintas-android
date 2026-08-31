package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;

public record CorporateFinancePageDto(
        List<CorporateFinanceSummaryDto> items,
        CorporateFinanceOverviewDto overview,
        String nextCursor) {
    public CorporateFinancePageDto {
        if (items == null
                || items.size() > 100
                || items.stream().anyMatch(java.util.Objects::isNull)
                || overview == null) {
            throw new IllegalArgumentException(
                    "Corporate finance page is invalid.");
        }
        items = List.copyOf(items);
        nextCursor = DtoValidation.optionalText(
                nextCursor,
                "Corporate finance cursor",
                128);
    }
}
