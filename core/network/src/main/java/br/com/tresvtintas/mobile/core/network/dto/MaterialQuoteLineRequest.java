package br.com.tresvtintas.mobile.core.network.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MaterialQuoteLineRequest(
        long productId,
        String quantity,
        Long colorId,
        Long tintContextId) {
    public MaterialQuoteLineRequest {
        productId = DtoValidation.requirePositive(productId, "Quote product ID");
        if (quantity == null || !quantity.matches("^\\d{1,8}(?:\\.\\d{1,2})?$")) {
            throw new IllegalArgumentException("Quote quantity is invalid.");
        }
        colorId = DtoValidation.optionalPositive(colorId, "Quote color ID");
        tintContextId = DtoValidation.optionalPositive(
                tintContextId, "Quote tint context ID");
        if ((colorId == null) != (tintContextId == null)) {
            throw new IllegalArgumentException(
                    "Quote tint identity must be supplied as a pair.");
        }
    }

    public MaterialQuoteLineRequest(long productId, String quantity) {
        this(productId, quantity, null, null);
    }
}
