package br.com.tresvtintas.mobile.core.network.dto;

public record OrderItemDto(
        long id,
        Long productId,
        String description,
        String quantity,
        String unit,
        String unitPrice,
        String total) {
    public OrderItemDto {
        id = DtoValidation.requirePositive(id, "Order item ID");
        productId = DtoValidation.optionalPositive(productId, "Order product ID");
        description = DtoValidation.requireText(
                description,
                "Order item description",
                300);
        if (quantity == null || !quantity.matches("^\\d{1,8}(?:\\.\\d{1,2})?$")) {
            throw new IllegalArgumentException("Order quantity is invalid.");
        }
        unit = DtoValidation.optionalText(unit, "Order unit", 20);
        if ((unitPrice == null) != (total == null)) {
            throw new IllegalArgumentException("Order item monetary values are inconsistent.");
        }
        if (unitPrice != null) {
            unitPrice = OrderSummaryDto.money(unitPrice);
            total = OrderSummaryDto.money(total);
        }
    }
}
