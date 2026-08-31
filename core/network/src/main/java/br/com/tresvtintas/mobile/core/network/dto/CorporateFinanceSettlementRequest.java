package br.com.tresvtintas.mobile.core.network.dto;

import java.util.Set;

public record CorporateFinanceSettlementRequest(
        String paymentMethod,
        String paymentReference,
        String confirmation) {
    private static final Set<String> METHODS =
            Set.of("pix", "transfer", "cash", "bank_slip", "other");

    public CorporateFinanceSettlementRequest {
        paymentMethod = DtoValidation.requireText(
                paymentMethod,
                "Corporate finance payment method",
                20);
        paymentReference = DtoValidation.optionalText(
                paymentReference,
                "Corporate finance payment reference",
                2_000);
        if (!METHODS.contains(paymentMethod)
                || !"SETTLE_CORPORATE_FINANCIAL_ENTRY".equals(confirmation)) {
            throw new IllegalArgumentException(
                    "Corporate finance settlement is invalid.");
        }
    }
}
