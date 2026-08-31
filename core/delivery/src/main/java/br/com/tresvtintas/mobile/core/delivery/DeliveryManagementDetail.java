package br.com.tresvtintas.mobile.core.delivery;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

public record DeliveryManagementDetail(
        DeliveryManagementSummary summary,
        Optional<Recipient> recipient,
        Destination destination,
        Optional<String> instructions,
        List<Item> items) {
    public DeliveryManagementDetail {
        Objects.requireNonNull(summary, "Managed delivery summary is required.");
        recipient = Objects.requireNonNull(
                recipient,
                "Managed delivery recipient is required.");
        Objects.requireNonNull(
                destination,
                "Managed delivery destination is required.");
        instructions = Objects.requireNonNull(
                instructions,
                "Managed delivery instructions are required.");
        if (items == null
                || items.size() > 200
                || items.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "Managed delivery items are invalid.");
        }
        items = List.copyOf(items);
    }

    public record Recipient(String name, Optional<String> phone) {
        public Recipient {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException(
                        "Managed delivery recipient is invalid.");
            }
            phone = Objects.requireNonNull(phone, "Recipient phone is required.");
        }
    }

    public record Destination(
            Optional<String> address,
            Optional<String> city,
            Optional<String> state) {
        public Destination {
            address = Objects.requireNonNull(address, "Address is required.");
            city = Objects.requireNonNull(city, "City is required.");
            state = Objects.requireNonNull(state, "State is required.");
        }
    }

    public record Item(
            long id,
            OptionalLong productId,
            String description,
            BigDecimal quantity,
            Optional<String> unit) {
        public Item {
            if (id < 1 || description == null || description.isBlank()
                    || quantity == null || quantity.signum() < 0) {
                throw new IllegalArgumentException(
                        "Managed delivery item is invalid.");
            }
            productId = Objects.requireNonNull(productId, "Product ID is required.");
            unit = Objects.requireNonNull(unit, "Item unit is required.");
        }
    }
}
