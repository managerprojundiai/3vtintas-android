package br.com.tresvtintas.mobile.core.network.dto;

public record DeliveryManagementOrganizationDto(long id, String name) {
    public DeliveryManagementOrganizationDto {
        id = DtoValidation.requirePositive(id, "Management organization ID");
        name = DtoValidation.requireText(
                name,
                "Management organization",
                200);
    }
}
