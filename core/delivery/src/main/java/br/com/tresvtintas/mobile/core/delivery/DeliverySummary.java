package br.com.tresvtintas.mobile.core.delivery;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record DeliverySummary(
        long id,
        DeliveryStatus status,
        Set<DeliveryAction> allowedActions,
        Order order,
        Optional<Organization> organization,
        Optional<Customer> customer,
        Optional<Driver> assignedDriver,
        Optional<Instant> scheduledAt,
        Optional<Instant> deliveredAt,
        Instant updatedAt) {
    private static final long MIN_IDENTIFIER = 1L;

    public DeliverySummary {
        if (id < MIN_IDENTIFIER) {
            throw new IllegalArgumentException("Delivery ID must be positive.");
        }
        Objects.requireNonNull(status, "Delivery status is required.");
        Objects.requireNonNull(order, "Delivery order is required.");
        Objects.requireNonNull(updatedAt, "Delivery update is required.");
        allowedActions = Set.copyOf(Objects.requireNonNull(
                allowedActions,
                "Delivery actions are required."));
        organization = requiredOptional(organization, "organization");
        customer = requiredOptional(customer, "customer");
        assignedDriver = requiredOptional(assignedDriver, "driver");
        scheduledAt = requiredOptional(scheduledAt, "schedule");
        deliveredAt = requiredOptional(deliveredAt, "completion");
    }

    private static <T> Optional<T> requiredOptional(
            Optional<T> value,
            String name) {
        return Objects.requireNonNull(value, "Delivery " + name + " is required.");
    }

    public record Order(long id, String status, int revision, int itemCount) {
        public Order {
            if (id < MIN_IDENTIFIER
                    || revision < MIN_IDENTIFIER
                    || itemCount < 0) {
                throw new IllegalArgumentException("Delivery order is invalid.");
            }
            status = requireText(status, "Delivery order status");
        }
    }

    public record Organization(long id, String name) {
        public Organization {
            if (id < MIN_IDENTIFIER) {
                throw new IllegalArgumentException("Delivery organization ID is invalid.");
            }
            name = requireText(name, "Delivery organization");
        }
    }

    public record Customer(
            long id,
            String name,
            Optional<String> city,
            Optional<String> state) {
        public Customer {
            if (id < MIN_IDENTIFIER) {
                throw new IllegalArgumentException("Delivery customer ID is invalid.");
            }
            name = requireText(name, "Delivery customer");
            city = Objects.requireNonNull(city, "Delivery city is required.");
            state = Objects.requireNonNull(state, "Delivery state is required.");
        }
    }

    public record Driver(
            long userId,
            Optional<String> name,
            boolean assignedToCurrentActor) {
        public Driver {
            if (userId < MIN_IDENTIFIER) {
                throw new IllegalArgumentException("Delivery driver ID is invalid.");
            }
            name = Objects.requireNonNull(name, "Delivery driver name is required.");
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required.");
        }
        return value;
    }
}
