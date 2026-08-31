package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;
import java.util.Set;

public record CorporateFinanceDetailDto(
        long id,
        String type,
        String status,
        String source,
        String title,
        String amount,
        String currency,
        String dueAt,
        String settledAt,
        CorporateFinanceOrganizationDto organization,
        CorporateFinanceSummaryDto.Customer customer,
        List<String> allowedActions,
        String createdAt,
        String updatedAt,
        String notes,
        Payment payment) {
    public CorporateFinanceDetailDto {
        new CorporateFinanceSummaryDto(
                id,
                type,
                status,
                source,
                title,
                amount,
                currency,
                dueAt,
                settledAt,
                organization,
                customer,
                allowedActions,
                createdAt,
                updatedAt);
        allowedActions = List.copyOf(allowedActions);
        notes = DtoValidation.optionalText(
                notes,
                "Corporate finance notes",
                20_000);
    }

    public record Payment(String method, String reference) {
        private static final Set<String> METHODS =
                Set.of("pix", "transfer", "cash", "bank_slip", "other");

        public Payment {
            method = DtoValidation.optionalText(
                    method,
                    "Corporate finance payment method",
                    20);
            reference = DtoValidation.optionalText(
                    reference,
                    "Corporate finance payment reference",
                    2_000);
            if (method != null && !METHODS.contains(method)) {
                throw new IllegalArgumentException(
                        "Corporate finance payment method is invalid.");
            }
        }
    }
}
