package br.com.tresvtintas.mobile.core.agent;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

public record AgentMaterialQuoteAmendChange(
        AgentMaterialQuoteAmendOperation operation,
        long productId,
        String description,
        Optional<BigDecimal> beforeQuantity,
        Optional<BigDecimal> afterQuantity) {
    public AgentMaterialQuoteAmendChange {
        operation = Objects.requireNonNull(
                operation,
                "Agent amendment operation is required.");
        if (productId < 1
                || description == null
                || description.isBlank()
                || description.length() > 300) {
            throw new IllegalArgumentException(
                    "Agent amendment change is invalid.");
        }
        beforeQuantity = requiredQuantity(
                beforeQuantity,
                "Agent amendment previous quantity is required.");
        afterQuantity = requiredQuantity(
                afterQuantity,
                "Agent amendment next quantity is required.");
        boolean valid = switch (operation) {
            case ADD -> beforeQuantity.isEmpty()
                    && afterQuantity.isPresent();
            case UPDATE_QUANTITY -> beforeQuantity.isPresent()
                    && afterQuantity.isPresent()
                    && beforeQuantity.get().compareTo(
                                    afterQuantity.get())
                            != 0;
            case REMOVE -> beforeQuantity.isPresent()
                    && afterQuantity.isEmpty();
        };
        if (!valid) {
            throw new IllegalArgumentException(
                    "Agent amendment change quantities are invalid.");
        }
    }

    private static Optional<BigDecimal> requiredQuantity(
            Optional<BigDecimal> value,
            String message) {
        Optional<BigDecimal> required = Objects.requireNonNull(
                value,
                message);
        required.ifPresent(quantity -> {
            if (quantity.signum() <= 0
                    || quantity.scale() > 2
                    || quantity.compareTo(
                                    new BigDecimal("99999999.99"))
                            > 0) {
                throw new IllegalArgumentException(message);
            }
        });
        return required;
    }
}
