package br.com.tresvtintas.mobile.core.network.dto;

import java.util.Set;

public record CorporateFinanceMutationResponse(
        long entryId,
        String status,
        Boolean changed) {
    public CorporateFinanceMutationResponse {
        entryId = DtoValidation.requirePositive(
                entryId,
                "Corporate finance mutation ID");
        status = DtoValidation.requireText(
                status,
                "Corporate finance mutation status",
                20);
        if (!Set.of("pending", "settled", "cancelled").contains(status)) {
            throw new IllegalArgumentException(
                    "Corporate finance mutation status is invalid.");
        }
    }
}
