package br.com.tresvtintas.mobile.core.network.dto;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record DeliverySummaryDto(
        long id,
        String status,
        List<String> allowedActions,
        Order order,
        Organization organization,
        Customer customer,
        Driver assignedDriver,
        String scheduledAt,
        String deliveredAt,
        String updatedAt) {
    private static final Set<String> STATUSES = Set.of(
            "pending", "shipped", "in_transit", "delivered", "failed");
    private static final Set<String> ACTIONS = Set.of("start", "complete");

    public DeliverySummaryDto {
        id = DtoValidation.requirePositive(id, "Delivery ID");
        if (!STATUSES.contains(status)
                || allowedActions == null
                || allowedActions.size() > 2
                || allowedActions.contains(null)
                || new HashSet<>(allowedActions).size() != allowedActions.size()
                || !ACTIONS.containsAll(allowedActions)) {
            throw new IllegalArgumentException("Delivery summary is invalid.");
        }
        allowedActions = List.copyOf(allowedActions);
        if (order == null) {
            throw new IllegalArgumentException("Delivery order is required.");
        }
        scheduledAt = optionalInstant(scheduledAt, "Delivery estimate");
        deliveredAt = optionalInstant(deliveredAt, "Delivery completion");
        updatedAt = DtoValidation.requireInstant(updatedAt, "Delivery update");
    }

    private static String optionalInstant(String value, String name) {
        if (value != null) {
            DtoValidation.requireInstant(value, name);
        }
        return value;
    }

    public record Order(long id, String status, int revision, int itemCount) {
        private static final Set<String> STATUSES = Set.of(
                "pending", "confirmed", "in_progress", "delivered", "cancelled");

        public Order {
            id = DtoValidation.requirePositive(id, "Delivery order ID");
            if (!STATUSES.contains(status) || revision < 1 || itemCount < 0) {
                throw new IllegalArgumentException("Delivery order is invalid.");
            }
        }
    }

    public record Organization(long id, String name) {
        public Organization {
            id = DtoValidation.requirePositive(id, "Delivery organization ID");
            name = DtoValidation.requireText(name, "Delivery organization", 200);
        }
    }

    public record Customer(
            long id,
            String name,
            String city,
            String state) {
        public Customer {
            id = DtoValidation.requirePositive(id, "Delivery customer ID");
            name = DtoValidation.requireText(name, "Delivery customer", 500);
            city = DtoValidation.optionalText(city, "Delivery city", 100);
            state = DtoValidation.optionalText(state, "Delivery state", 2);
        }
    }

    public record Driver(
            long userId,
            String name,
            boolean assignedToCurrentActor) {
        public Driver {
            userId = DtoValidation.requirePositive(userId, "Delivery driver ID");
            name = DtoValidation.optionalText(name, "Delivery driver", 500);
        }
    }
}
