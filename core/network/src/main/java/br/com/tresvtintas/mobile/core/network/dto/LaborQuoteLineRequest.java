package br.com.tresvtintas.mobile.core.network.dto;

public record LaborQuoteLineRequest(
        String description,
        String quantity,
        String unit,
        String unitPrice) {
    public LaborQuoteLineRequest {
        description = DtoValidation.requireText(
                description,
                "Labor quote line description",
                300).trim();
        unit = DtoValidation.requireText(unit, "Labor quote unit", 20).trim();
        quantity = LaborQuoteSummaryDto.inputMoney(quantity);
        unitPrice = LaborQuoteSummaryDto.inputMoney(unitPrice);
    }
}
