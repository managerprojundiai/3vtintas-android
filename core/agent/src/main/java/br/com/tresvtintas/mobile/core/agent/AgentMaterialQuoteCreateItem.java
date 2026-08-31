package br.com.tresvtintas.mobile.core.agent;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record AgentMaterialQuoteCreateItem(
        long productId,
        String description,
        BigDecimal quantity,
        String unit,
        BigDecimal unitPrice,
        BigDecimal total) {
    private static final long MINIMUM_PRODUCT_ID = 1;
    private static final int MAXIMUM_QUANTITY_SCALE = 2;
    private static final BigDecimal MAXIMUM_AMOUNT =
            new BigDecimal("99999999.99");

    public AgentMaterialQuoteCreateItem {
        if (productId < MINIMUM_PRODUCT_ID) {
            throw new IllegalArgumentException(
                    "Agent action product ID is invalid.");
        }
        description = requireText(description, 300);
        quantity = requirePositiveDecimal(
                quantity,
                "Agent action quantity is invalid.");
        if (quantity.scale() > MAXIMUM_QUANTITY_SCALE) {
            throw new IllegalArgumentException(
                    "Agent action quantity is invalid.");
        }
        unit = requireText(unit, 20);
        unitPrice = requireMoney(unitPrice);
        total = requireMoney(total);
        if (unitPrice.multiply(quantity)
                        .setScale(2, RoundingMode.HALF_UP)
                        .compareTo(total)
                != 0) {
            throw new IllegalArgumentException(
                    "Agent action line total is invalid.");
        }
    }

    private static String requireText(String value, int maximum) {
        if (value == null
                || value.isBlank()
                || value.length() > maximum) {
            throw new IllegalArgumentException(
                    "Agent action line text is invalid.");
        }
        return value;
    }

    private static BigDecimal requirePositiveDecimal(
            BigDecimal value,
            String message) {
        BigDecimal required = Objects.requireNonNull(value, message);
        if (required.signum() <= 0
                || required.compareTo(MAXIMUM_AMOUNT) > 0) {
            throw new IllegalArgumentException(message);
        }
        return required;
    }

    private static BigDecimal requireMoney(BigDecimal value) {
        BigDecimal required = Objects.requireNonNull(
                value,
                "Agent action money is required.");
        if (required.scale() != 2
                || required.signum() < 0
                || required.compareTo(MAXIMUM_AMOUNT) > 0) {
            throw new IllegalArgumentException(
                    "Agent action money is invalid.");
        }
        return required;
    }
}
