package br.com.tresvtintas.mobile.core.order;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public record OrderDetail(
        OrderSummary summary,
        Optional<String> notes,
        Optional<OrderPricingSnapshot> pricing,
        Optional<CustomerContact> customerContact,
        Optional<DeliveryDetails> deliveryDetails,
        List<OrderItem> items) {
    private static final int MAX_ITEMS = 200;

    public OrderDetail {
        if (summary == null) {
            throw new IllegalArgumentException("Order detail summary is required.");
        }
        notes = notes == null ? Optional.empty() : notes;
        pricing = pricing == null ? Optional.empty() : pricing;
        customerContact = customerContact == null
                ? Optional.empty()
                : customerContact;
        deliveryDetails = deliveryDetails == null
                ? Optional.empty()
                : deliveryDetails;
        if (items != null && items.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("Order detail items are invalid.");
        }
        items = items == null ? List.of() : List.copyOf(items);
        if (items.size() > MAX_ITEMS) {
            throw new IllegalArgumentException("Order detail items are invalid.");
        }
    }

    public record CustomerContact(
            long id,
            String name,
            Optional<String> email,
            Optional<String> phone,
            Optional<String> address,
            Optional<String> city,
            Optional<String> state) {
        public CustomerContact {
            if (id < 1 || name == null || name.isBlank()) {
                throw new IllegalArgumentException("Order customer contact is invalid.");
            }
            email = optional(email);
            phone = optional(phone);
            address = optional(address);
            city = optional(city);
            state = optional(state);
        }
    }

    public record DeliveryDetails(
            Optional<String> address,
            Optional<String> notes,
            Optional<String> trackingCode,
            Optional<Instant> estimatedAt,
            Optional<Instant> deliveredAt) {
        public DeliveryDetails {
            address = optional(address);
            notes = optional(notes);
            trackingCode = optional(trackingCode);
            estimatedAt = estimatedAt == null ? Optional.empty() : estimatedAt;
            deliveredAt = deliveredAt == null ? Optional.empty() : deliveredAt;
        }
    }

    private static Optional<String> optional(Optional<String> value) {
        return value == null ? Optional.empty() : value;
    }
}
