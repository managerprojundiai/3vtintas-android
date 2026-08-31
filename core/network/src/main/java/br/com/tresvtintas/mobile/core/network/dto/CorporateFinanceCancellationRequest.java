package br.com.tresvtintas.mobile.core.network.dto;

public record CorporateFinanceCancellationRequest(String confirmation) {
    public CorporateFinanceCancellationRequest {
        if (!"CANCEL_CORPORATE_FINANCIAL_ENTRY".equals(confirmation)) {
            throw new IllegalArgumentException(
                    "Corporate finance cancellation is invalid.");
        }
    }
}
