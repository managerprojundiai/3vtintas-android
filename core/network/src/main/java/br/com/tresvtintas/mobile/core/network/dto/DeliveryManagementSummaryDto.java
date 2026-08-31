package br.com.tresvtintas.mobile.core.network.dto;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record DeliveryManagementSummaryDto(
        Order order,
        DeliveryManagementOrganizationDto organization,
        Customer customer,
        Delivery delivery,
        List<String> allowedActions) {
    private static final Set<String> ACTIONS = Set.of(
            "schedule", "assign", "unassign", "complete");

    public DeliveryManagementSummaryDto {
        if (order == null || organization == null
                || allowedActions == null
                || allowedActions.size() > 4
                || allowedActions.stream().anyMatch(Objects::isNull)
                || new HashSet<>(allowedActions).size()
                        != allowedActions.size()
                || !ACTIONS.containsAll(allowedActions)) {
            throw new IllegalArgumentException(
                    "Management delivery summary is invalid.");
        }
        allowedActions = List.copyOf(allowedActions);
    }

    public record Order(
            long id,
            String status,
            int revision,
            int itemCount,
            String createdAt,
            String updatedAt) {
        private static final Set<String> STATUSES = Set.of(
                "pending", "confirmed", "in_progress",
                "delivered", "cancelled");

        public Order {
            id = DtoValidation.requirePositive(id, "Management order ID");
            if (!STATUSES.contains(status)
                    || revision < 1
                    || itemCount < 0) {
                throw new IllegalArgumentException(
                        "Management order is invalid.");
            }
            createdAt = DtoValidation.requireInstant(
                    createdAt,
                    "Management order creation");
            updatedAt = DtoValidation.requireInstant(
                    updatedAt,
                    "Management order update");
        }
    }

    public record Customer(
            long id,
            String name,
            String city,
            String state) {
        public Customer {
            id = DtoValidation.requirePositive(id, "Management customer ID");
            name = DtoValidation.requireText(
                    name,
                    "Management customer",
                    500);
            city = DtoValidation.optionalText(
                    city,
                    "Management customer city",
                    100);
            state = DtoValidation.optionalText(
                    state,
                    "Management customer state",
                    2);
        }
    }

    public record Delivery(
            long id,
            String status,
            String scheduledAt,
            DeliveryManagementDriverDto assignedDriver) {
        private static final Set<String> STATUSES = Set.of(
                "pending", "shipped", "in_transit", "delivered", "failed");

        public Delivery {
            id = DtoValidation.requirePositive(id, "Managed delivery ID");
            if (!STATUSES.contains(status)) {
                throw new IllegalArgumentException(
                        "Managed delivery status is invalid.");
            }
            if (scheduledAt != null) {
                scheduledAt = DtoValidation.requireInstant(
                        scheduledAt,
                        "Managed delivery schedule");
            }
        }
    }
}
