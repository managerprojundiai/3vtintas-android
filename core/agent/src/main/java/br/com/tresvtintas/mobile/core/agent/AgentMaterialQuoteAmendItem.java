package br.com.tresvtintas.mobile.core.agent;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record AgentMaterialQuoteAmendItem(
        long productId,
        String description,
        BigDecimal quantity,
        String unit,
        BigDecimal unitPrice,
        BigDecimal total) {
    private static final long MINIMUM_PRODUCT_ID = 1;
    private static final BigDecimal MAXIMUM_AMOUNT =
            new BigDecimal("99999999.99");

    public AgentMaterialQuoteAmendItem {
        if (productId < MINIMUM_PRODUCT_ID) {
            throw new IllegalArgumentException(
                    "Agent amendment product ID is invalid.");
        }
        description = requireText(description, 300);
        quantity = requireQuantity(quantity);
        unit = requireText(unit, 20);
        unitPrice = requireMoney(unitPrice);
        total = requireMoney(total);
        if (unitPrice.multiply(quantity)
                        .setScale(2, RoundingMode.HALF_UP)
                        .compareTo(total)
                != 0) {
            throw new IllegalArgumentException(
                    "Agent amendment line total is invalid.");
        }
    }

    private static String requireText(String value, int maximum) {
        if (value == null
                || value.isBlank()
                || value.length() > maximum) {
            throw new IllegalArgumentException(
                    "Agent amendment line text is invalid.");
        }
        return value;
    }

    private static BigDecimal requireQuantity(BigDecimal value) {
        BigDecimal required = Objects.requireNonNull(
                value,
                "Agent amendment quantity is required.");
        if (required.signum() <= 0
                || required.scale() > 2
                || required.compareTo(MAXIMUM_AMOUNT) > 0) {
            throw new IllegalArgumentException(
                    "Agent amendment quantity is invalid.");
        }
        return required;
    }

    private static BigDecimal requireMoney(BigDecimal value) {
        BigDecimal required = Objects.requireNonNull(
                value,
                "Agent amendment money is required.");
        if (required.scale() != 2
                || required.signum() < 0
                || required.compareTo(MAXIMUM_AMOUNT) > 0) {
            throw new IllegalArgumentException(
                    "Agent amendment money is invalid.");
        }
        return required;
    }
}
