package br.com.tresvtintas.mobile.core.network.dto;

public record PersonalFinanceCancellationRequest(String confirmation) {
    public PersonalFinanceCancellationRequest {
        if (!"CANCEL_PERSONAL_FINANCIAL_ENTRY".equals(confirmation)) {
            throw new IllegalArgumentException(
                    "Personal finance cancellation is invalid.");
        }
    }
}
