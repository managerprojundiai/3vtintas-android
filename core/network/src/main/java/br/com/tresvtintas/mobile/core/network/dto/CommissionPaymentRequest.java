package br.com.tresvtintas.mobile.core.network.dto;

import java.util.Set;

public record CommissionPaymentRequest(
        int expectedRevision,
        boolean confirmed,
        String paymentMethod,
        String paymentReference) {
    private static final Set<String> METHODS = Set.of(
            "pix",
            "transfer",
            "cash",
            "bank_slip",
            "other");

    public CommissionPaymentRequest {
        if (paymentReference != null) {
            paymentReference = paymentReference.trim();
        }
        if (expectedRevision < 1
                || !confirmed
                || paymentMethod == null
                || !METHODS.contains(paymentMethod)
                || (paymentReference != null
                        && (paymentReference.isEmpty()
                                || paymentReference.length() > 255))) {
            throw new IllegalArgumentException(
                    "Commission payment request is invalid.");
        }
    }
}
