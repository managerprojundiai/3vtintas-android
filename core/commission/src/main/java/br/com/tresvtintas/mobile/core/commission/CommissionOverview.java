package br.com.tresvtintas.mobile.core.commission;

import java.math.BigDecimal;

public record CommissionOverview(
        Totals pending,
        Totals approved,
        Totals paid,
        Totals cancelled) {
    public CommissionOverview {
        if (pending == null || approved == null || paid == null || cancelled == null) {
            throw new IllegalArgumentException("Commission overview is invalid.");
        }
    }

    public record Totals(int count, BigDecimal amount) {
        public Totals {
            if (count < 0 || amount == null || amount.signum() < 0) {
                throw new IllegalArgumentException("Commission totals are invalid.");
            }
        }
    }
}
