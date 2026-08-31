package br.com.tresvtintas.mobile.core.network.dto;

public record PersonalFinanceTypeTotalsDto(
        PersonalFinanceMoneyTotalDto expense,
        PersonalFinanceMoneyTotalDto payable,
        PersonalFinanceMoneyTotalDto receivable) {
    public PersonalFinanceTypeTotalsDto {
        if (expense == null || payable == null || receivable == null) {
            throw new IllegalArgumentException(
                    "Personal finance type totals are invalid.");
        }
    }
}
