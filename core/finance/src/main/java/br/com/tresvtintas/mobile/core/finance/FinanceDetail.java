package br.com.tresvtintas.mobile.core.finance;

import java.util.Optional;

public record FinanceDetail(
        FinanceSummary summary,
        Optional<String> notes,
        Optional<Payment> payment) {
    public FinanceDetail {
        notes = notes == null ? Optional.empty() : notes;
        payment = payment == null ? Optional.empty() : payment;
        if (summary == null
                || notes.filter(value -> value.length() > 20_000).isPresent()) {
            throw new IllegalArgumentException("Finance detail is invalid.");
        }
    }

    public record Payment(
            Optional<FinancePaymentMethod> method,
            Optional<String> reference) {
        public Payment {
            method = method == null ? Optional.empty() : method;
            reference = reference == null ? Optional.empty() : reference;
            if (reference.filter(value -> value.length() > 2_000).isPresent()) {
                throw new IllegalArgumentException("Finance payment is invalid.");
            }
        }
    }
}
