package br.com.tresvtintas.mobile.core.network.dto;

public record CommissionTotalsDto(int count, String amount) {
    public CommissionTotalsDto {
        if (count < 0) {
            throw new IllegalArgumentException("Commission count is invalid.");
        }
        if (amount == null || !amount.matches("^\\d{1,18}\\.\\d{2}$")) {
            throw new IllegalArgumentException(
                    "Commission aggregate amount is invalid.");
        }
    }
}
