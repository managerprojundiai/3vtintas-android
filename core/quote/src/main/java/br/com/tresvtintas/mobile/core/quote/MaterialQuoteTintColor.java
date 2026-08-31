package br.com.tresvtintas.mobile.core.quote;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public record MaterialQuoteTintColor(
        long productId,
        String productName,
        Optional<String> productSku,
        Optional<String> productUnit,
        Optional<String> productBrand,
        long colorId,
        String colorPublicId,
        String colorName,
        Optional<String> hexColor,
        long tintContextId,
        String tintContextPublicId,
        MaterialQuoteTintConfiguration configuration,
        BigDecimal amount) {
    public MaterialQuoteTintColor {
        if (productId < 1 || colorId < 1 || tintContextId < 1
                || productName == null || productName.isBlank()
                || productName.length() > 255
                || colorName == null || colorName.isBlank()
                || colorName.length() > 255
                || configuration == null || amount == null
                || amount.signum() < 0 || amount.scale() != 2) {
            throw new IllegalArgumentException("Sellable tint color is invalid.");
        }
        productName = productName.trim();
        colorName = colorName.trim();
        colorPublicId = UUID.fromString(colorPublicId).toString();
        tintContextPublicId = UUID.fromString(tintContextPublicId).toString();
        productSku = normalize(productSku, 64);
        productUnit = normalize(productUnit, 64);
        productBrand = normalize(productBrand, 160);
        hexColor = normalize(hexColor, 7);
        if (hexColor.isPresent()
                && !hexColor.orElseThrow().matches("^#[0-9A-Fa-f]{6}$")) {
            throw new IllegalArgumentException("Tint color hexadecimal value is invalid.");
        }
    }

    public MaterialQuoteTintSelection selection() {
        return new MaterialQuoteTintSelection(
                colorId,
                colorName,
                tintContextId,
                configuration.lineName(),
                configuration.finishName(),
                configuration.packageName());
    }

    private static Optional<String> normalize(
            Optional<String> value,
            int maximumLength) {
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }
        String normalized = value.orElseThrow().trim();
        if (normalized.isEmpty() || normalized.length() > maximumLength) {
            throw new IllegalArgumentException("Tint product metadata is invalid.");
        }
        return Optional.of(normalized);
    }
}
