package br.com.tresvtintas.mobile.core.delivery;

import java.util.Objects;
import java.util.Optional;

public record DeliveryManagementDriver(
        long userId,
        Optional<String> name) {
    private static final long MINIMUM_USER_ID = 1;

    public DeliveryManagementDriver {
        if (userId < MINIMUM_USER_ID) {
            throw new IllegalArgumentException(
                    "Delivery management driver is invalid.");
        }
        name = Objects.requireNonNull(name, "Driver name is required.");
    }

    public String displayName() {
        return name.filter(value -> !value.isBlank())
                .orElse("Entregador #" + userId);
    }
}
