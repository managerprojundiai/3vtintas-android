package br.com.tresvtintas.mobile.core.network.dto;

public record DeliveryManagementDriverDto(long userId, String name) {
    public DeliveryManagementDriverDto {
        userId = DtoValidation.requirePositive(
                userId,
                "Management driver ID");
        name = DtoValidation.optionalText(name, "Management driver", 500);
    }
}
