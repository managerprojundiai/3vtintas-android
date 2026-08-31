package br.com.tresvtintas.mobile.core.network.dto;

public record CorporateFinanceMoneyTotalDto(int count, String amount) {
    public CorporateFinanceMoneyTotalDto {
        if (count < 0) {
            throw new IllegalArgumentException(
                    "Corporate finance total count is invalid.");
        }
        amount = CorporateFinanceSummaryDto.money(amount, 18);
    }
}
