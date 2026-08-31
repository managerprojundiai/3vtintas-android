package br.com.tresvtintas.mobile.core.network.dto;

import java.math.BigDecimal;
import java.util.Set;

public record AgentActionChangeDto(
        String operation,
        long productId,
        String description,
        String beforeQuantity,
        String afterQuantity) {
    private static final Set<String> OPERATIONS = Set.of(
            "add",
            "update_quantity",
            "remove");

    public AgentActionChangeDto {
        if (!OPERATIONS.contains(operation)) {
            throw new IllegalArgumentException(
                    "Agent amendment operation is invalid.");
        }
        productId = DtoValidation.requirePositive(
                productId,
                "Agent amendment product ID");
        description = DtoValidation.requireText(
                description,
                "Agent amendment product",
                300);
        requireOptionalQuantity(beforeQuantity);
        requireOptionalQuantity(afterQuantity);
        boolean valid = switch (operation) {
            case "add" -> beforeQuantity == null
                    && afterQuantity != null;
            case "update_quantity" -> beforeQuantity != null
                    && afterQuantity != null
                    && new BigDecimal(beforeQuantity).compareTo(
                                    new BigDecimal(afterQuantity))
                            != 0;
            case "remove" -> beforeQuantity != null
                    && afterQuantity == null;
            default -> false;
        };
        if (!valid) {
            throw new IllegalArgumentException(
                    "Agent amendment quantities are invalid.");
        }
    }

    private static void requireOptionalQuantity(String value) {
        if (value != null
                && (!value.matches("^\\d{1,8}(?:\\.\\d{1,2})?$")
                        || new BigDecimal(value).signum() <= 0)) {
            throw new IllegalArgumentException(
                    "Agent amendment quantity is invalid.");
        }
    }
}
