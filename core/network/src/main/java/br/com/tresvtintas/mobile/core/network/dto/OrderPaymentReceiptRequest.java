package br.com.tresvtintas.mobile.core.network.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderPaymentReceiptRequest(
        int expectedRevision,
        String paymentMethod,
        String paymentReference,
        boolean confirmed) {
    private static final Set<String> METHODS = Set.of(
            "pix",
            "transfer",
            "cash",
            "bank_slip",
            "other");

    public OrderPaymentReceiptRequest {
        if (expectedRevision < 1
                || !METHODS.contains(paymentMethod)
                || !confirmed) {
            throw new IllegalArgumentException(
                    "Order payment receipt request is invalid.");
        }
        paymentReference = DtoValidation.optionalText(
                paymentReference,
                "Order payment reference",
                500);
    }
}
