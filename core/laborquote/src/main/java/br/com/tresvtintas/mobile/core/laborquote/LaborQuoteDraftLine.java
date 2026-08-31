package br.com.tresvtintas.mobile.core.laborquote;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record LaborQuoteDraftLine(
        String description,
        BigDecimal quantity,
        String unit,
        BigDecimal unitPrice) {
    public LaborQuoteDraftLine {
        description = normalize(description, 300, "description");
        unit = normalize(unit, 20, "unit");
        if (quantity == null
                || quantity.signum() <= 0
                || quantity.scale() > 2
                || unitPrice == null
                || unitPrice.signum() < 0
                || unitPrice.scale() > 2) {
            throw new IllegalArgumentException("Labor quote values are invalid.");
        }
        quantity = quantity.setScale(2);
        unitPrice = unitPrice.setScale(2);
    }

    public BigDecimal total() {
        return quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
    }

    private static String normalize(String value, int maximum, String field) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty() || normalized.length() > maximum) {
            throw new IllegalArgumentException("Labor quote " + field + " is invalid.");
        }
        return normalized;
    }
}
