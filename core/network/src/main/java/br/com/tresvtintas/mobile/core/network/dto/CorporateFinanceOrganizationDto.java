package br.com.tresvtintas.mobile.core.network.dto;

public record CorporateFinanceOrganizationDto(long id, String name) {
    public CorporateFinanceOrganizationDto {
        id = DtoValidation.requirePositive(
                id,
                "Corporate finance organization ID");
        name = DtoValidation.requireText(
                name,
                "Corporate finance organization name",
                200);
    }
}
