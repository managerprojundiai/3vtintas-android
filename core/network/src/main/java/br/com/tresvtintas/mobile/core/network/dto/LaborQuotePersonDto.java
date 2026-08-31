package br.com.tresvtintas.mobile.core.network.dto;

public record LaborQuotePersonDto(long id, String name) {
    public LaborQuotePersonDto {
        id = DtoValidation.requirePositive(id, "Labor quote person ID");
        name = DtoValidation.requireText(name, "Labor quote person name", 200);
    }
}
