package br.com.tresvtintas.mobile.core.delivery;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record DeliveryManagementSummary(
        Order order,
        DeliveryManagementOrganization organization,
        Optional<Customer> customer,
        Optional<ManagedDelivery> delivery,
        Set<DeliveryManagementAction> allowedActions) {
    private static final long MINIMUM_ID = 1;

    public DeliveryManagementSummary {
        Objects.requireNonNull(order, "Managed delivery order is required.");
        Objects.requireNonNull(
                organization,
                "Managed delivery organization is required.");
        customer = Objects.requireNonNull(
                customer,
                "Managed delivery customer is required.");
        delivery = Objects.requireNonNull(
                delivery,
                "Managed delivery is required.");
        allowedActions = Set.copyOf(Objects.requireNonNull(
                allowedActions,
                "Managed delivery actions are required."));
    }

    public record Order(
            long id,
            String status,
            int revision,
            int itemCount,
            Instant createdAt,
            Instant updatedAt) {
        public Order {
            if (id < MINIMUM_ID || revision < MINIMUM_ID || itemCount < 0
                    || status == null || status.isBlank()) {
                throw new IllegalArgumentException(
                        "Managed delivery order is invalid.");
            }
            Objects.requireNonNull(createdAt, "Order creation is required.");
            Objects.requireNonNull(updatedAt, "Order update is required.");
        }
    }

    public record Customer(
            long id,
            String name,
            Optional<String> city,
            Optional<String> state) {
        public Customer {
            if (id < MINIMUM_ID || name == null || name.isBlank()) {
                throw new IllegalArgumentException(
                        "Managed delivery customer is invalid.");
            }
            city = Objects.requireNonNull(city, "Customer city is required.");
            state = Objects.requireNonNull(state, "Customer state is required.");
        }
    }

    public record ManagedDelivery(
            long id,
            DeliveryStatus status,
            Optional<Instant> scheduledAt,
            Optional<DeliveryManagementDriver> assignedDriver) {
        public ManagedDelivery {
            if (id < MINIMUM_ID) {
                throw new IllegalArgumentException(
                        "Managed delivery ID is invalid.");
            }
            Objects.requireNonNull(status, "Managed delivery status is required.");
            scheduledAt = Objects.requireNonNull(
                    scheduledAt,
                    "Managed delivery schedule is required.");
            assignedDriver = Objects.requireNonNull(
                    assignedDriver,
                    "Managed delivery driver is required.");
        }
    }
}
