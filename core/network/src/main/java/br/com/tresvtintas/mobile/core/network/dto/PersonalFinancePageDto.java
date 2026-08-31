package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;

public record PersonalFinancePageDto(
        List<PersonalFinanceSummaryDto> items,
        PersonalFinanceOverviewDto overview,
        String nextCursor) {
    public PersonalFinancePageDto {
        if (items == null
                || items.size() > 100
                || items.stream().anyMatch(java.util.Objects::isNull)
                || overview == null) {
            throw new IllegalArgumentException(
                    "Personal finance page is invalid.");
        }
        items = List.copyOf(items);
        nextCursor = DtoValidation.optionalText(
                nextCursor,
                "Personal finance cursor",
                96);
    }
}
