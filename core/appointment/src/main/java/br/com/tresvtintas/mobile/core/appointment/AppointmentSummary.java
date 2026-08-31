package br.com.tresvtintas.mobile.core.appointment;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

public record AppointmentSummary(
        long id,
        AppointmentKind kind,
        AppointmentStatus status,
        String title,
        Instant scheduledAt,
        int durationMinutes,
        Optional<String> location,
        AppointmentPerson responsible,
        Optional<Organization> organization,
        Optional<Customer> customer,
        Optional<Order> order,
        long revision,
        Set<AppointmentAction> allowedActions,
        Instant createdAt,
        Instant updatedAt) {
    public AppointmentSummary {
        location = location == null ? Optional.empty() : location;
        organization = organization == null ? Optional.empty() : organization;
        customer = customer == null ? Optional.empty() : customer;
        order = order == null ? Optional.empty() : order;
        if (id < 1
                || kind == null
                || status == null
                || title == null
                || title.isBlank()
                || title.length() > 200
                || scheduledAt == null
                || durationMinutes < 1
                || durationMinutes > 1_440
                || location.filter(value -> value.length() > 5_000).isPresent()
                || responsible == null
                || revision < 1
                || allowedActions == null
                || allowedActions.stream().anyMatch(java.util.Objects::isNull)
                || createdAt == null
                || updatedAt == null) {
            throw new IllegalArgumentException(
                    "Appointment summary is invalid.");
        }
        allowedActions = Set.copyOf(allowedActions);
        if ((kind == AppointmentKind.DELIVERY || status.isTerminal())
                && !allowedActions.isEmpty()) {
            throw new IllegalArgumentException(
                    "Read-only appointments cannot expose actions.");
        }
    }

    public record Organization(long id, String name) {
        public Organization {
            if (id < 1 || name == null || name.isBlank()) {
                throw new IllegalArgumentException(
                        "Appointment organization is invalid.");
            }
        }
    }

    public record Customer(long id, String name) {
        public Customer {
            if (id < 1 || name == null || name.isBlank()) {
                throw new IllegalArgumentException(
                        "Appointment customer is invalid.");
            }
        }
    }

    public record Order(long id, String type, String status) {
        public Order {
            if (id < 1
                    || (!"material".equals(type) && !"labor".equals(type))
                    || !Set.of(
                                    "pending",
                                    "confirmed",
                                    "in_progress",
                                    "delivered",
                                    "cancelled")
                            .contains(status)) {
                throw new IllegalArgumentException(
                        "Appointment order is invalid.");
            }
        }
    }
}
