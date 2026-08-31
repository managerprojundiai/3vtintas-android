package br.com.tresvtintas.mobile.core.network.dto;

public record CommissionOverviewDto(
        CommissionTotalsDto pending,
        CommissionTotalsDto approved,
        CommissionTotalsDto paid,
        CommissionTotalsDto cancelled) {
    public CommissionOverviewDto {
        if (pending == null
                || approved == null
                || paid == null
                || cancelled == null) {
            throw new IllegalArgumentException("Commission overview is invalid.");
        }
    }
}
