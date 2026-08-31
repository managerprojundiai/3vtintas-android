package br.com.tresvtintas.mobile.core.network.dto;

public record CorporateFinanceOverviewDto(
        CorporateFinanceTypeTotalsDto pending,
        CorporateFinanceTypeTotalsDto settled,
        CorporateFinanceTypeTotalsDto cancelled,
        CorporateFinanceTypeTotalsDto overdue) {
    public CorporateFinanceOverviewDto {
        if (pending == null
                || settled == null
                || cancelled == null
                || overdue == null) {
            throw new IllegalArgumentException(
                    "Corporate finance overview is invalid.");
        }
    }
}
