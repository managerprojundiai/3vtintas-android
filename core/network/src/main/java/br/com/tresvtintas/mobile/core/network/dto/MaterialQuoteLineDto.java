package br.com.tresvtintas.mobile.core.network.dto;

public record MaterialQuoteLineDto(
        long id,
        Long productId,
        String description,
        String quantity,
        String unit,
        String unitPrice,
        String total,
        MaterialQuoteTintSnapshotDto tint,
        MaterialQuoteProductStateDto currentProduct) {
    public MaterialQuoteLineDto {
        id = DtoValidation.requirePositive(id, "Quote line ID");
        productId = DtoValidation.optionalPositive(productId, "Quote product ID");
        description = DtoValidation.requireText(
                description,
                "Quote line description",
                300);
        quantity = MaterialQuoteSummaryDto.money(quantity);
        unit = DtoValidation.optionalText(unit, "Quote unit", 20);
        unitPrice = MaterialQuoteSummaryDto.money(unitPrice);
        total = MaterialQuoteSummaryDto.money(total);
    }
}
