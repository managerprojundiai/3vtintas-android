package br.com.tresvtintas.mobile.core.network.dto;

import java.util.Set;

public record AgentFinanceResultDto(
        String scope,
        long entryId,
        String status,
        boolean changed) {
    public AgentFinanceResultDto {
        if (!Set.of("personal", "corporate").contains(scope)
                || !Set.of("pending", "settled", "cancelled")
                        .contains(status)) {
            throw new IllegalArgumentException(
                    "Agent finance result is invalid.");
        }
        entryId = DtoValidation.requirePositive(
                entryId,
                "Agent finance result entry ID");
    }
}
