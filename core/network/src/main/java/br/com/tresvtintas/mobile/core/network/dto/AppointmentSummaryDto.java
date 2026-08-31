package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;
import java.util.Set;

public record AppointmentSummaryDto(
        long id,
        String kind,
        String status,
        String title,
        String scheduledAt,
        int durationMinutes,
        String location,
        AppointmentPersonDto responsible,
        Organization organization,
        Customer customer,
        Order order,
        long revision,
        List<String> allowedActions,
        String createdAt,
        String updatedAt) {
    private static final Set<String> KINDS =
            Set.of("general", "delivery", "collection");
    private static final Set<String> STATUSES =
            Set.of("scheduled", "confirmed", "completed", "cancelled");
    private static final Set<String> ACTIONS =
            Set.of("update", "confirm", "complete", "cancel");

    public AppointmentSummaryDto {
        id = DtoValidation.requirePositive(id, "Appointment ID");
        kind = DtoValidation.requireText(kind, "Appointment kind", 20);
        status = DtoValidation.requireText(status, "Appointment status", 20);
        title = DtoValidation.requireText(title, "Appointment title", 200);
        scheduledAt = DtoValidation.requireInstant(
                scheduledAt,
                "Appointment schedule");
        location = DtoValidation.optionalText(
                location,
                "Appointment location",
                5_000);
        createdAt = DtoValidation.requireInstant(
                createdAt,
                "Appointment creation");
        updatedAt = DtoValidation.requireInstant(
                updatedAt,
                "Appointment update");
        if (!KINDS.contains(kind)
                || !STATUSES.contains(status)
                || durationMinutes < 1
                || durationMinutes > 1_440
                || responsible == null
                || revision < 1
                || allowedActions == null
                || allowedActions.size() > 4
                || allowedActions.stream().anyMatch(
                        value -> value == null || !ACTIONS.contains(value))
                || Set.copyOf(allowedActions).size() != allowedActions.size()) {
            throw new IllegalArgumentException(
                    "Appointment summary is invalid.");
        }
        allowedActions = List.copyOf(allowedActions);
        if (("delivery".equals(kind)
                        || "completed".equals(status)
                        || "cancelled".equals(status))
                && !allowedActions.isEmpty()) {
            throw new IllegalArgumentException(
                    "Read-only appointment actions are invalid.");
        }
    }

    public record Organization(long id, String name) {
        public Organization {
            id = DtoValidation.requirePositive(
                    id,
                    "Appointment organization ID");
            name = DtoValidation.requireText(
                    name,
                    "Appointment organization name",
                    255);
        }
    }

    public record Customer(long id, String name) {
        public Customer {
            id = DtoValidation.requirePositive(
                    id,
                    "Appointment customer ID");
            name = DtoValidation.requireText(
                    name,
                    "Appointment customer name",
                    500);
        }
    }

    public record Order(long id, String type, String status) {
        public Order {
            id = DtoValidation.requirePositive(id, "Appointment order ID");
            type = DtoValidation.requireText(
                    type,
                    "Appointment order type",
                    20);
            status = DtoValidation.requireText(
                    status,
                    "Appointment order status",
                    30);
            if (!Set.of("material", "labor").contains(type)
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
