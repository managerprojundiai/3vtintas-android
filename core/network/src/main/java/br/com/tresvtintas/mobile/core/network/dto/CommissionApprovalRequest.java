package br.com.tresvtintas.mobile.core.network.dto;

public record CommissionApprovalRequest(
        int expectedRevision,
        boolean confirmed) {
    public CommissionApprovalRequest {
        if (expectedRevision < 1 || !confirmed) {
            throw new IllegalArgumentException(
                    "Commission approval request is invalid.");
        }
    }
}
