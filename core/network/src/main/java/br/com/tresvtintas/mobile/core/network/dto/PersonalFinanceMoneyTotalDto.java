package br.com.tresvtintas.mobile.core.network.dto;

public record PersonalFinanceMoneyTotalDto(int count, String amount) {
    public PersonalFinanceMoneyTotalDto {
        if (count < 0) {
            throw new IllegalArgumentException(
                    "Personal finance total count is invalid.");
        }
        amount = PersonalFinanceSummaryDto.money(amount, 18);
    }
}
