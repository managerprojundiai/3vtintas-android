package br.com.tresvtintas.mobile.core.delivery;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

public record DeliveryDetail(
        DeliverySummary summary,
        Optional<Recipient> recipient,
        Destination destination,
        Optional<String> instructions,
        Optional<String> trackingCode,
        List<Item> items) {
    private static final long MIN_IDENTIFIER = 1L;

    public DeliveryDetail {
        Objects.requireNonNull(summary, "Delivery summary is required.");
        recipient = Objects.requireNonNull(recipient, "Delivery recipient is required.");
        Objects.requireNonNull(destination, "Delivery destination is required.");
        instructions = Objects.requireNonNull(instructions, "Delivery instructions are required.");
        trackingCode = Objects.requireNonNull(trackingCode, "Delivery tracking is required.");
        if (items == null
                || items.stream().anyMatch(Objects::isNull)
                || items.size() > 200) {
            throw new IllegalArgumentException("Delivery items are invalid.");
        }
        items = List.copyOf(items);
    }

    public record Recipient(String name, Optional<String> phone) {
        public Recipient {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Delivery recipient name is required.");
            }
            phone = Objects.requireNonNull(phone, "Delivery recipient phone is required.");
        }
    }

    public record Destination(
            Optional<String> address,
            Optional<String> city,
            Optional<String> state) {
        public Destination {
            address = Objects.requireNonNull(address, "Delivery address is required.");
            city = Objects.requireNonNull(city, "Delivery city is required.");
            state = Objects.requireNonNull(state, "Delivery state is required.");
        }
    }

    public record Item(
            long id,
            OptionalLong productId,
            String description,
            BigDecimal quantity,
            Optional<String> unit) {
        public Item {
            if (id < MIN_IDENTIFIER
                    || quantity == null || quantity.signum() < 0
                    || description == null || description.isBlank()) {
                throw new IllegalArgumentException("Delivery item is invalid.");
            }
            productId = Objects.requireNonNull(productId, "Delivery product ID is required.");
            unit = Objects.requireNonNull(unit, "Delivery item unit is required.");
        }
    }
}
