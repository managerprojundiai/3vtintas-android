package br.com.tresvtintas.mobile.core.dashboard;

import java.math.BigDecimal;
import java.util.Objects;

public record DashboardCountAmount(int count, BigDecimal amount) {
    private static final int MONEY_SCALE = 2;

    public DashboardCountAmount {
        Objects.requireNonNull(amount, "Dashboard amount is required.");
        if (count < 0
                || amount.signum() < 0
                || amount.scale() != MONEY_SCALE) {
            throw new IllegalArgumentException(
                    "Dashboard aggregate is invalid.");
        }
    }
}
