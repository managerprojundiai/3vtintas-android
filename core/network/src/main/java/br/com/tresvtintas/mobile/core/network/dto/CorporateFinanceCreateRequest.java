package br.com.tresvtintas.mobile.core.network.dto;

import java.util.Set;

public record CorporateFinanceCreateRequest(
        long organizationId,
        String type,
        String title,
        String amount,
        String dueAt,
        Long customerId,
        String notes,
        String confirmation) {
    private static final Set<String> TYPES =
            Set.of("expense", "payable", "receivable");

    public CorporateFinanceCreateRequest {
        organizationId = DtoValidation.requirePositive(
                organizationId,
                "Corporate finance organization ID");
        type = DtoValidation.requireText(type, "Corporate finance type", 20);
        title = DtoValidation.requireText(title, "Corporate finance title", 200);
        amount = CorporateFinanceSummaryDto.money(amount, 8);
        if (!TYPES.contains(type)
                || !"CREATE_CORPORATE_FINANCIAL_ENTRY".equals(confirmation)) {
            throw new IllegalArgumentException(
                    "Corporate finance creation is invalid.");
        }
        if (dueAt != null) {
            DtoValidation.requireInstant(dueAt, "Corporate finance due date");
        }
        customerId = DtoValidation.optionalPositive(
                customerId,
                "Corporate finance customer ID");
        notes = DtoValidation.optionalText(
                notes,
                "Corporate finance notes",
                20_000);
    }
}
