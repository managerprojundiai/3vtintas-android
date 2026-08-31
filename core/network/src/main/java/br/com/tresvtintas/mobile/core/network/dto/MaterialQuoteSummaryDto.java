package br.com.tresvtintas.mobile.core.network.dto;

public record MaterialQuoteSummaryDto(
        long id,
        MaterialQuoteCustomerDto customer,
        Long organizationId,
        String title,
        String status,
        String subtotal,
        String discount,
        String total,
        int revision,
        int itemCount,
        String validUntil,
        String createdAt,
        String updatedAt) {
    public MaterialQuoteSummaryDto {
        id = DtoValidation.requirePositive(id, "Quote ID");
        if (customer == null || revision < 1 || itemCount < 1 || itemCount > 100) {
            throw new IllegalArgumentException("Quote summary is invalid.");
        }
        organizationId = DtoValidation.optionalPositive(
                organizationId,
                "Quote organization ID");
        title = DtoValidation.requireText(title, "Quote title", 200);
        status = DtoValidation.requireText(status, "Quote status", 20);
        subtotal = money(subtotal);
        discount = money(discount);
        total = money(total);
        validUntil = optionalInstant(validUntil);
        createdAt = DtoValidation.requireInstant(createdAt, "Quote creation");
        updatedAt = DtoValidation.requireInstant(updatedAt, "Quote update");
    }

    static String money(String value) {
        if (value == null || !value.matches("^\\d{1,8}\\.\\d{2}$")) {
            throw new IllegalArgumentException("Quote monetary value is invalid.");
        }
        return value;
    }

    private static String optionalInstant(String value) {
        if (value != null) {
            DtoValidation.requireInstant(value, "Quote expiration");
        }
        return value;
    }
}
