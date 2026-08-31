package br.com.tresvtintas.mobile.core.network.dto;

public record LaborQuoteSummaryDto(
        long id,
        LaborQuotePersonDto customer,
        LaborQuotePersonDto painter,
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
    public LaborQuoteSummaryDto {
        id = DtoValidation.requirePositive(id, "Labor quote ID");
        if (customer == null || painter == null || revision < 1 || itemCount < 1 || itemCount > 100) {
            throw new IllegalArgumentException("Labor quote summary is invalid.");
        }
        organizationId = DtoValidation.optionalPositive(
                organizationId,
                "Labor quote organization ID");
        title = DtoValidation.requireText(title, "Labor quote title", 200);
        if (!LaborQuoteValidation.STATUSES.contains(status)) {
            throw new IllegalArgumentException("Labor quote status is invalid.");
        }
        subtotal = money(subtotal);
        discount = money(discount);
        total = money(total);
        validUntil = optionalInstant(validUntil);
        createdAt = DtoValidation.requireInstant(createdAt, "Labor quote creation");
        updatedAt = DtoValidation.requireInstant(updatedAt, "Labor quote update");
    }

    static String money(String value) {
        if (value == null || !value.matches("^\\d{1,8}\\.\\d{2}$")) {
            throw new IllegalArgumentException("Labor quote monetary value is invalid.");
        }
        return value;
    }

    static String inputMoney(String value) {
        if (value == null || !value.matches("^\\d{1,8}(?:\\.\\d{1,2})?$")) {
            throw new IllegalArgumentException("Labor quote input value is invalid.");
        }
        return value;
    }

    private static String optionalInstant(String value) {
        if (value != null) {
            DtoValidation.requireInstant(value, "Labor quote expiration");
        }
        return value;
    }
}
