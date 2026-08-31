package br.com.tresvtintas.mobile.core.network.dto;

import java.util.Set;

public record AgentCommissionResultDto(
        long commissionId,
        String status,
        int revision,
        boolean changed) {
    public AgentCommissionResultDto {
        commissionId = DtoValidation.requirePositive(
                commissionId,
                "Agent commission result ID");
        if (revision < 1
                || !Set.of("pending", "approved", "paid", "cancelled")
                        .contains(status)) {
            throw new IllegalArgumentException(
                    "Agent commission result is invalid.");
        }
    }
}
