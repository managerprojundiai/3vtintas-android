package br.com.tresvtintas.mobile.core.quote;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

public record MaterialQuoteLine(
        long id,
        OptionalLong productId,
        String description,
        BigDecimal quantity,
        Optional<String> unit,
        BigDecimal unitPrice,
        BigDecimal total,
        Optional<MaterialQuoteTintSnapshot> tint,
        Optional<Boolean> productActive,
        OptionalInt currentStock) {
    private static final int MINIMUM_IDENTIFIER = 1;

    public MaterialQuoteLine {
        if (id < MINIMUM_IDENTIFIER) {
            throw new IllegalArgumentException("Quote line ID is invalid.");
        }
        productId = productId == null ? OptionalLong.empty() : productId;
        if (productId.isPresent()
                && productId.getAsLong() < MINIMUM_IDENTIFIER) {
            throw new IllegalArgumentException("Quote product ID is invalid.");
        }
        if (description == null || description.isBlank() || description.length() > 300) {
            throw new IllegalArgumentException("Quote line description is invalid.");
        }
        unit = unit == null ? Optional.empty() : unit;
        Objects.requireNonNull(quantity, "Quote quantity is required.");
        Objects.requireNonNull(unitPrice, "Quote unit price is required.");
        Objects.requireNonNull(total, "Quote line total is required.");
        tint = tint == null ? Optional.empty() : tint;
        productActive = productActive == null ? Optional.empty() : productActive;
        currentStock = currentStock == null ? OptionalInt.empty() : currentStock;
    }
}
