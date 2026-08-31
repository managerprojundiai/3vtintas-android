package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;

public record PersonalFinanceDetailDto(
        long id,
        String type,
        String status,
        String source,
        String title,
        String amount,
        String currency,
        String dueAt,
        String settledAt,
        PersonalFinanceSummaryDto.Customer customer,
        List<String> allowedActions,
        String createdAt,
        String updatedAt,
        String notes,
        Payment payment) {
    public PersonalFinanceDetailDto {
        new PersonalFinanceSummaryDto(
                id,
                type,
                status,
                source,
                title,
                amount,
                currency,
                dueAt,
                settledAt,
                customer,
                allowedActions,
                createdAt,
                updatedAt);
        allowedActions = List.copyOf(allowedActions);
        notes = DtoValidation.optionalText(
                notes,
                "Personal finance notes",
                20_000);
    }

    public record Payment(String method, String reference) {
        public Payment {
            method = DtoValidation.optionalText(
                    method,
                    "Personal finance payment method",
                    20);
            reference = DtoValidation.optionalText(
                    reference,
                    "Personal finance payment reference",
                    2_000);
            if (method != null
                    && !java.util.Set.of(
                                    "pix",
                                    "transfer",
                                    "cash",
                                    "bank_slip",
                                    "other")
                            .contains(method)) {
                throw new IllegalArgumentException(
                        "Personal finance payment method is invalid.");
            }
        }
    }
}
