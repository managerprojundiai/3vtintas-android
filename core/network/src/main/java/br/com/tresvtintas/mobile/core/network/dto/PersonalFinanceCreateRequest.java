package br.com.tresvtintas.mobile.core.network.dto;

import java.util.Set;

public record PersonalFinanceCreateRequest(
        String type,
        String title,
        String amount,
        String dueAt,
        Long customerId,
        String notes,
        String confirmation) {
    private static final Set<String> TYPES =
            Set.of("expense", "payable", "receivable");

    public PersonalFinanceCreateRequest {
        type = DtoValidation.requireText(type, "Personal finance type", 20);
        title = DtoValidation.requireText(title, "Personal finance title", 200);
        amount = PersonalFinanceSummaryDto.money(amount, 8);
        if (!TYPES.contains(type)
                || !"CREATE_PERSONAL_FINANCIAL_ENTRY".equals(confirmation)) {
            throw new IllegalArgumentException(
                    "Personal finance creation is invalid.");
        }
        if (dueAt != null) {
            DtoValidation.requireInstant(dueAt, "Personal finance due date");
        }
        customerId = DtoValidation.optionalPositive(
                customerId,
                "Personal finance customer ID");
        notes = DtoValidation.optionalText(
                notes,
                "Personal finance notes",
                20_000);
    }
}
