package br.com.tresvtintas.mobile.core.network.dto;

public record LaborQuoteLineDto(
        long id,
        String description,
        String quantity,
        String unit,
        String unitPrice,
        String total) {
    public LaborQuoteLineDto {
        id = DtoValidation.requirePositive(id, "Labor quote line ID");
        description = DtoValidation.requireText(
                description,
                "Labor quote line description",
                300);
        quantity = LaborQuoteSummaryDto.money(quantity);
        unit = DtoValidation.requireText(unit, "Labor quote unit", 20);
        unitPrice = LaborQuoteSummaryDto.money(unitPrice);
        total = LaborQuoteSummaryDto.money(total);
    }
}
