package br.com.tresvtintas.mobile.core.network.dto;

import java.util.Set;

public record PersonalFinanceSettlementRequest(
        String paymentMethod,
        String paymentReference,
        String confirmation) {
    private static final Set<String> METHODS =
            Set.of("pix", "transfer", "cash", "bank_slip", "other");

    public PersonalFinanceSettlementRequest {
        paymentMethod = DtoValidation.requireText(
                paymentMethod,
                "Personal finance payment method",
                20);
        paymentReference = DtoValidation.optionalText(
                paymentReference,
                "Personal finance payment reference",
                2_000);
        if (!METHODS.contains(paymentMethod)
                || !"SETTLE_PERSONAL_FINANCIAL_ENTRY".equals(confirmation)) {
            throw new IllegalArgumentException(
                    "Personal finance settlement is invalid.");
        }
    }
}
