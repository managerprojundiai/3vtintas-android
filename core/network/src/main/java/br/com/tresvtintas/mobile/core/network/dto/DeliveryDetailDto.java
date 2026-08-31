package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;

public record DeliveryDetailDto(
        long id,
        String status,
        List<String> allowedActions,
        DeliverySummaryDto.Order order,
        DeliverySummaryDto.Organization organization,
        DeliverySummaryDto.Customer customer,
        DeliverySummaryDto.Driver assignedDriver,
        String scheduledAt,
        String deliveredAt,
        String updatedAt,
        Recipient recipient,
        Destination destination,
        String instructions,
        String trackingCode,
        List<Item> items) {
    public DeliveryDetailDto {
        new DeliverySummaryDto(
                id,
                status,
                allowedActions,
                order,
                organization,
                customer,
                assignedDriver,
                scheduledAt,
                deliveredAt,
                updatedAt);
        allowedActions = List.copyOf(allowedActions);
        if (destination == null
                || items == null
                || items.size() > 200
                || items.contains(null)) {
            throw new IllegalArgumentException("Delivery detail is invalid.");
        }
        instructions = DtoValidation.optionalText(
                instructions,
                "Delivery instructions",
                10_000);
        trackingCode = DtoValidation.optionalText(
                trackingCode,
                "Delivery tracking code",
                100);
        items = List.copyOf(items);
    }

    public record Recipient(String name, String phone) {
        public Recipient {
            name = DtoValidation.requireText(name, "Delivery recipient", 500);
            phone = DtoValidation.optionalText(phone, "Delivery phone", 20);
        }
    }

    public record Destination(String address, String city, String state) {
        public Destination {
            address = DtoValidation.optionalText(address, "Delivery address", 5_000);
            city = DtoValidation.optionalText(city, "Delivery city", 100);
            state = DtoValidation.optionalText(state, "Delivery state", 2);
        }
    }

    public record Item(
            long id,
            Long productId,
            String description,
            String quantity,
            String unit) {
        public Item {
            id = DtoValidation.requirePositive(id, "Delivery item ID");
            productId = DtoValidation.optionalPositive(
                    productId,
                    "Delivery product ID");
            description = DtoValidation.requireText(
                    description,
                    "Delivery item",
                    300);
            if (quantity == null
                    || !quantity.matches("^\\d{1,8}(?:\\.\\d{1,2})?$")) {
                throw new IllegalArgumentException("Delivery quantity is invalid.");
            }
            unit = DtoValidation.optionalText(unit, "Delivery unit", 20);
        }
    }
}
