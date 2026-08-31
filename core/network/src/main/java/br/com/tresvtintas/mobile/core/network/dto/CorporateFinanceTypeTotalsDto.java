package br.com.tresvtintas.mobile.core.network.dto;

public record CorporateFinanceTypeTotalsDto(
        CorporateFinanceMoneyTotalDto expense,
        CorporateFinanceMoneyTotalDto payable,
        CorporateFinanceMoneyTotalDto receivable) {
    public CorporateFinanceTypeTotalsDto {
        if (expense == null || payable == null || receivable == null) {
            throw new IllegalArgumentException(
                    "Corporate finance type totals are invalid.");
        }
    }
}
