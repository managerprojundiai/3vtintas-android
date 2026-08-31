package br.com.tresvtintas.mobile.core.quote;

import java.math.BigDecimal;
import java.util.Optional;

public record MaterialQuoteDraftLine(
        long productId,
        String productName,
        BigDecimal quantity,
        BigDecimal displayedUnitPrice,
        Optional<MaterialQuoteTintSelection> tint) {
    public MaterialQuoteDraftLine {
        if (productId < 1
                || productName == null
                || productName.isBlank()
                || quantity == null
                || quantity.signum() <= 0
                || quantity.scale() > 2
                || displayedUnitPrice == null
                || displayedUnitPrice.signum() < 0) {
            throw new IllegalArgumentException("Quote draft line is invalid.");
        }
        quantity = quantity.setScale(2);
        displayedUnitPrice = displayedUnitPrice.setScale(2);
        tint = tint == null ? Optional.empty() : tint;
    }

    public MaterialQuoteDraftLine(
            long productId,
            String productName,
            BigDecimal quantity,
            BigDecimal displayedUnitPrice) {
        this(
                productId,
                productName,
                quantity,
                displayedUnitPrice,
                Optional.empty());
    }

    public BigDecimal estimate() {
        return quantity.multiply(displayedUnitPrice).setScale(
                2,
                java.math.RoundingMode.HALF_UP);
    }

    public String identityKey() {
        return tint.map(value -> value.identityKey(productId))
                .orElse(productId + ":ready");
    }
}
