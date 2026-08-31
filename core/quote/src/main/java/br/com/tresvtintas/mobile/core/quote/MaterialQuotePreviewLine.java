package br.com.tresvtintas.mobile.core.quote;

import java.math.BigDecimal;
import java.util.Objects;

public record MaterialQuotePreviewLine(
        long productId,
        String description,
        BigDecimal quantity,
        String unit,
        BigDecimal unitPrice,
        BigDecimal total) {
    public MaterialQuotePreviewLine {
        if (productId < 1 || description == null || description.isBlank()
                || description.length() > 300 || unit == null || unit.isBlank()
                || unit.length() > 20) {
            throw new IllegalArgumentException("Quote preview line is invalid.");
        }
        requireNonNegativeMoney(quantity, "Preview quantity");
        requireNonNegativeMoney(unitPrice, "Preview unit price");
        requireNonNegativeMoney(total, "Preview total");
    }

    private static void requireNonNegativeMoney(BigDecimal value, String name) {
        Objects.requireNonNull(value, name + " is required.");
        if (value.signum() < 0 || value.scale() != 2 || value.precision() > 10) {
            throw new IllegalArgumentException(name + " is invalid.");
        }
    }
}
