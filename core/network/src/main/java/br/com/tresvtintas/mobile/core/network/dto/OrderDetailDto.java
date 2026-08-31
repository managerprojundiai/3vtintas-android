package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;

public record OrderDetailDto(
        long id,
        String type,
        String status,
        int revision,
        String paymentStatus,
        List<String> allowedActions,
        String total,
        int itemCount,
        Long quoteId,
        OrderSummaryDto.Organization organization,
        OrderSummaryDto.Seller seller,
        OrderSummaryDto.Customer customer,
        OrderSummaryDto.Delivery delivery,
        String createdAt,
        String updatedAt,
        String notes,
        OrderPricingSnapshotDto pricing,
        CustomerContact customerContact,
        DeliveryDetails deliveryDetails,
        List<OrderItemDto> items) {
    public OrderDetailDto {
        new OrderSummaryDto(
                id,
                type,
                status,
                revision,
                paymentStatus,
                allowedActions,
                total,
                itemCount,
                quoteId,
                organization,
                seller,
                customer,
                delivery,
                createdAt,
                updatedAt);
        allowedActions = List.copyOf(allowedActions);
        notes = DtoValidation.optionalText(notes, "Order notes", 10_000);
        if (items == null || items.size() > 200 || items.contains(null)) {
            throw new IllegalArgumentException("Order detail items are invalid.");
        }
        items = List.copyOf(items);
    }

    public record CustomerContact(
            long id,
            String name,
            String email,
            String phone,
            String address,
            String city,
            String state) {
        public CustomerContact {
            id = DtoValidation.requirePositive(id, "Order contact ID");
            name = DtoValidation.requireText(name, "Order contact name", 200);
            email = DtoValidation.optionalText(email, "Order contact email", 320);
            phone = DtoValidation.optionalText(phone, "Order contact phone", 20);
            address = DtoValidation.optionalText(address, "Order contact address", 2_000);
            city = DtoValidation.optionalText(city, "Order contact city", 100);
            state = DtoValidation.optionalText(state, "Order contact state", 2);
        }
    }

    public record DeliveryDetails(
            String address,
            String notes,
            String trackingCode,
            String estimatedAt,
            String deliveredAt) {
        public DeliveryDetails {
            address = DtoValidation.optionalText(address, "Delivery address", 5_000);
            notes = DtoValidation.optionalText(notes, "Delivery notes", 10_000);
            trackingCode = DtoValidation.optionalText(
                    trackingCode,
                    "Delivery tracking code",
                    100);
            if (estimatedAt != null) {
                DtoValidation.requireInstant(estimatedAt, "Delivery estimate");
            }
            if (deliveredAt != null) {
                DtoValidation.requireInstant(deliveredAt, "Delivery completion");
            }
        }
    }
}
