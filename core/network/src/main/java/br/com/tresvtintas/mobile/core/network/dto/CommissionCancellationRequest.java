package br.com.tresvtintas.mobile.core.network.dto;

public record CommissionCancellationRequest(
        int expectedRevision,
        boolean confirmed,
        String reason) {
    public CommissionCancellationRequest {
        if (reason == null) {
            throw new IllegalArgumentException(
                    "Commission cancellation reason is required.");
        }
        reason = reason.trim();
        if (expectedRevision < 1
                || !confirmed
                || reason.length() < 10
                || reason.length() > 500) {
            throw new IllegalArgumentException(
                    "Commission cancellation request is invalid.");
        }
    }
}
