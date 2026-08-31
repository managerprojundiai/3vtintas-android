package br.com.tresvtintas.mobile.core.network.dto;

import java.util.Set;

public record CommissionMutationResponse(
        long commissionId,
        String status,
        int revision,
        boolean changed) {
    private static final Set<String> STATUSES =
            Set.of("pending", "approved", "paid", "cancelled");

    public CommissionMutationResponse {
        if (commissionId < 1
                || !STATUSES.contains(status)
                || revision < 1) {
            throw new IllegalArgumentException(
                    "Commission mutation response is invalid.");
        }
    }
}
