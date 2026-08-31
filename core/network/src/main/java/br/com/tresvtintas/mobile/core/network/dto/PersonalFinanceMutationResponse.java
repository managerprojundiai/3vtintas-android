package br.com.tresvtintas.mobile.core.network.dto;

import java.util.Set;

public record PersonalFinanceMutationResponse(
        long entryId,
        String status,
        Boolean changed) {
    public PersonalFinanceMutationResponse {
        entryId = DtoValidation.requirePositive(
                entryId,
                "Personal finance mutation ID");
        status = DtoValidation.requireText(
                status,
                "Personal finance mutation status",
                20);
        if (!Set.of("pending", "settled", "cancelled").contains(status)) {
            throw new IllegalArgumentException(
                    "Personal finance mutation status is invalid.");
        }
    }
}
