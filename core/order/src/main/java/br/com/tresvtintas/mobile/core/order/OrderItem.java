package br.com.tresvtintas.mobile.core.order;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.OptionalLong;

public record OrderItem(
        long id,
        OptionalLong productId,
        String description,
        BigDecimal quantity,
        Optional<String> unit,
        Optional<BigDecimal> unitPrice,
        Optional<BigDecimal> total) {
    public OrderItem {
        if (id < 1
                || description == null
                || description.isBlank()
                || quantity == null
                || quantity.signum() <= 0
                || unitPrice == null
                || unitPrice.filter(value -> value.signum() < 0).isPresent()
                || total == null
                || total.filter(value -> value.signum() < 0).isPresent()
                || unitPrice.isPresent() != total.isPresent()) {
            throw new IllegalArgumentException("Order item is invalid.");
        }
        productId = productId == null ? OptionalLong.empty() : productId;
        unit = unit == null ? Optional.empty() : unit;
    }
}
