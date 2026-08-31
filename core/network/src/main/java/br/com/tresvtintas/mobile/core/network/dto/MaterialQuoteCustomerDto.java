package br.com.tresvtintas.mobile.core.network.dto;

public record MaterialQuoteCustomerDto(long id, String name) {
    public MaterialQuoteCustomerDto {
        id = DtoValidation.requirePositive(id, "Quote customer ID");
        name = DtoValidation.requireText(name, "Quote customer name", 200);
    }
}
