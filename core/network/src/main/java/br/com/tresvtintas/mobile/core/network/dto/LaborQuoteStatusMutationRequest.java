package br.com.tresvtintas.mobile.core.network.dto;

public record LaborQuoteStatusMutationRequest(int expectedRevision, String status) {
    public LaborQuoteStatusMutationRequest {
        if (expectedRevision < 1 || !LaborQuoteValidation.MANUAL_STATUSES.contains(status)) {
            throw new IllegalArgumentException("Labor quote status request is invalid.");
        }
    }
}
