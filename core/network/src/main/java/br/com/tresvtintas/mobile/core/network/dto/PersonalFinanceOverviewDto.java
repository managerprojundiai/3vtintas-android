package br.com.tresvtintas.mobile.core.network.dto;

public record PersonalFinanceOverviewDto(
        PersonalFinanceTypeTotalsDto pending,
        PersonalFinanceTypeTotalsDto settled,
        PersonalFinanceTypeTotalsDto cancelled,
        PersonalFinanceTypeTotalsDto overdue) {
    public PersonalFinanceOverviewDto {
        if (pending == null
                || settled == null
                || cancelled == null
                || overdue == null) {
            throw new IllegalArgumentException(
                    "Personal finance overview is invalid.");
        }
    }
}
