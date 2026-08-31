package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;
import java.util.Objects;

public record DeliveryManagementDetailDto(
        DeliveryManagementSummaryDto.Order order,
        DeliveryManagementOrganizationDto organization,
        DeliveryManagementSummaryDto.Customer customer,
        DeliveryManagementSummaryDto.Delivery delivery,
        List<String> allowedActions,
        Recipient recipient,
        Destination destination,
        String instructions,
        List<Item> items) {
    public DeliveryManagementDetailDto {
        new DeliveryManagementSummaryDto(
                order,
                organization,
                customer,
                delivery,
                allowedActions);
        allowedActions = List.copyOf(allowedActions);
        if (destination == null
                || items == null
                || items.size() > 200
                || items.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "Management delivery detail is invalid.");
        }
        instructions = DtoValidation.optionalText(
                instructions,
                "Management delivery instructions",
                10_000);
        items = List.copyOf(items);
    }

    public record Recipient(String name, String phone) {
        public Recipient {
            name = DtoValidation.requireText(
                    name,
                    "Management recipient",
                    500);
            phone = DtoValidation.optionalText(
                    phone,
                    "Management recipient phone",
                    20);
        }
    }

    public record Destination(String address, String city, String state) {
        public Destination {
            address = DtoValidation.optionalText(
                    address,
                    "Management destination",
                    5_000);
            city = DtoValidation.optionalText(
                    city,
                    "Management destination city",
                    100);
            state = DtoValidation.optionalText(
                    state,
                    "Management destination state",
                    2);
        }
    }

    public record Item(
            long id,
            Long productId,
            String description,
            String quantity,
            String unit) {
        public Item {
            id = DtoValidation.requirePositive(id, "Management item ID");
            productId = DtoValidation.optionalPositive(
                    productId,
                    "Management product ID");
            description = DtoValidation.requireText(
                    description,
                    "Management item",
                    300);
            if (quantity == null
                    || !quantity.matches("^\\d{1,8}(?:\\.\\d{1,2})?$")) {
                throw new IllegalArgumentException(
                        "Management item quantity is invalid.");
            }
            unit = DtoValidation.optionalText(
                    unit,
                    "Management item unit",
                    20);
        }
    }
}
