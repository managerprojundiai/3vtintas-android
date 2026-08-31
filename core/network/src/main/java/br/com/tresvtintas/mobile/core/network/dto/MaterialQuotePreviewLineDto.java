package br.com.tresvtintas.mobile.core.network.dto;

public record MaterialQuotePreviewLineDto(
        long productId,
        String description,
        String quantity,
        String unit,
        String unitPrice,
        String total,
        Long colorId,
        Long tintContextId) {
    public MaterialQuotePreviewLineDto {
        productId = DtoValidation.requirePositive(productId, "Preview product ID");
        description = DtoValidation.requireText(
                description, "Preview line description", 300);
        quantity = MaterialQuoteSummaryDto.money(quantity);
        unit = DtoValidation.requireText(unit, "Preview line unit", 20);
        unitPrice = MaterialQuoteSummaryDto.money(unitPrice);
        total = MaterialQuoteSummaryDto.money(total);
        colorId = DtoValidation.optionalPositive(colorId, "Preview color ID");
        tintContextId = DtoValidation.optionalPositive(
                tintContextId, "Preview tint context ID");
        if ((colorId == null) != (tintContextId == null)) {
            throw new IllegalArgumentException("Preview tint identity is incomplete.");
        }
    }
}
