package br.com.tresvtintas.mobile.core.network.dto;

public record OrderConversionResponse(long quoteId, int quoteRevision, long orderId,
        int orderRevision, String status, String total, long deliveryId, Seller seller) {
    public OrderConversionResponse {
        quoteId = DtoValidation.requirePositive(quoteId, "Conversion quote ID");
        orderId = DtoValidation.requirePositive(orderId, "Conversion order ID");
        deliveryId = DtoValidation.requirePositive(deliveryId, "Conversion delivery ID");
        if (quoteRevision < 1 || orderRevision < 1 || seller == null) {
            throw new IllegalArgumentException("Order conversion is invalid.");
        }
        status = DtoValidation.requireText(status, "Conversion status", 30);
        total = OrderSummaryDto.money(total);
    }
    public record Seller(long userId, String role) {
        public Seller {
            userId = DtoValidation.requirePositive(userId, "Conversion seller ID");
            role = DtoValidation.requireText(role, "Conversion seller role", 20);
        }
    }
}
