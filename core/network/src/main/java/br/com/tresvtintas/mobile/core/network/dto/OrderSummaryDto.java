package br.com.tresvtintas.mobile.core.network.dto;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record OrderSummaryDto(
        long id,
        String type,
        String status,
        int revision,
        String paymentStatus,
        List<String> allowedActions,
        String total,
        int itemCount,
        Long quoteId,
        Organization organization,
        Seller seller,
        Customer customer,
        Delivery delivery,
        String createdAt,
        String updatedAt) {
    public OrderSummaryDto {
        id = DtoValidation.requirePositive(id, "Order ID");
        type = DtoValidation.requireText(type, "Order type", 20);
        status = DtoValidation.requireText(status, "Order status", 30);
        if (revision < 1 || itemCount < 0 || itemCount > 200) {
            throw new IllegalArgumentException("Order summary is invalid.");
        }
        paymentStatus = DtoValidation.requireText(
                paymentStatus,
                "Order payment status",
                20);
        if (allowedActions == null
                || allowedActions.size() > 5
                || allowedActions.stream().anyMatch(java.util.Objects::isNull)
                || new HashSet<>(allowedActions).size() != allowedActions.size()
                || !ACTIONS.containsAll(allowedActions)) {
            throw new IllegalArgumentException("Order allowed actions are invalid.");
        }
        allowedActions = List.copyOf(allowedActions);
        if (total != null) {
            total = money(total);
        }
        quoteId = DtoValidation.optionalPositive(quoteId, "Order quote ID");
        createdAt = DtoValidation.requireInstant(createdAt, "Order creation");
        updatedAt = DtoValidation.requireInstant(updatedAt, "Order update");
    }

    private static final Set<String> ACTIONS = Set.of(
            "confirm",
            "start_fulfillment",
            "complete",
            "cancel",
            "record_payment");

    static String money(String value) {
        if (value == null || !value.matches("^\\d{1,8}\\.\\d{2}$")) {
            throw new IllegalArgumentException("Order monetary value is invalid.");
        }
        return value;
    }

    public record Organization(long id, String name) {
        public Organization {
            id = DtoValidation.requirePositive(id, "Order organization ID");
            name = DtoValidation.requireText(name, "Order organization name", 200);
        }
    }

    public record Seller(long userId, String role, String name) {
        public Seller {
            userId = DtoValidation.requirePositive(userId, "Order seller ID");
            role = DtoValidation.requireText(role, "Order seller role", 20);
            name = DtoValidation.optionalText(name, "Order seller name", 500);
        }
    }

    public record Customer(long id, String name) {
        public Customer {
            id = DtoValidation.requirePositive(id, "Order customer ID");
            name = DtoValidation.requireText(name, "Order customer name", 200);
        }
    }

    public record Delivery(
            long id,
            String status,
            String estimatedAt,
            boolean assignedToCurrentActor) {
        public Delivery {
            id = DtoValidation.requirePositive(id, "Order delivery ID");
            status = DtoValidation.requireText(status, "Order delivery status", 30);
            if (estimatedAt != null) {
                DtoValidation.requireInstant(estimatedAt, "Order delivery estimate");
            }
        }
    }
}
