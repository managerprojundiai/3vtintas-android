package br.com.tresvtintas.mobile.core.laborquote;

import java.math.BigDecimal;
import java.util.Objects;

public record LaborQuoteLine(
        long id,
        String description,
        BigDecimal quantity,
        String unit,
        BigDecimal unitPrice,
        BigDecimal total) {
    public LaborQuoteLine {
        if (id < 1
                || description == null
                || description.isBlank()
                || description.length() > 300
                || unit == null
                || unit.isBlank()
                || unit.length() > 20) {
            throw new IllegalArgumentException("Labor quote line is invalid.");
        }
        validateMoney(quantity, false);
        validateMoney(unitPrice, true);
        validateMoney(total, true);
    }

    private static void validateMoney(BigDecimal value, boolean zeroAllowed) {
        Objects.requireNonNull(value, "Labor quote number is required.");
        if (value.scale() != 2 || (zeroAllowed ? value.signum() < 0 : value.signum() <= 0)) {
            throw new IllegalArgumentException("Labor quote number is invalid.");
        }
    }
}
