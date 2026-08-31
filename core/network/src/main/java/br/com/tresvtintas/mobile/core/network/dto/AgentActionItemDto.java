package br.com.tresvtintas.mobile.core.network.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record AgentActionItemDto(
        long productId,
        String description,
        String quantity,
        String unit,
        String unitPrice,
        String total) {
    public AgentActionItemDto {
        productId = DtoValidation.requirePositive(
                productId,
                "Agent action product ID");
        description = DtoValidation.requireText(
                description,
                "Agent action product",
                300);
        unit = DtoValidation.requireText(
                unit,
                "Agent action unit",
                20);
        requireQuantity(quantity);
        requireMoney(unitPrice);
        requireMoney(total);
        if (new BigDecimal(quantity)
                        .multiply(new BigDecimal(unitPrice))
                        .setScale(2, RoundingMode.HALF_UP)
                        .compareTo(new BigDecimal(total))
                != 0) {
            throw new IllegalArgumentException(
                    "Agent action item total is invalid.");
        }
    }

    private static void requireQuantity(String value) {
        if (value == null
                || !value.matches("^\\d{1,8}(?:\\.\\d{1,2})?$")
                || new BigDecimal(value).signum() <= 0) {
            throw new IllegalArgumentException(
                    "Agent action quantity is invalid.");
        }
    }

    private static void requireMoney(String value) {
        if (value == null || !value.matches("^\\d{1,8}\\.\\d{2}$")) {
            throw new IllegalArgumentException(
                    "Agent action money is invalid.");
        }
    }
}
