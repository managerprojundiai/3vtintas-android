package br.com.tresvtintas.mobile.core.network.dto;

public record CustomerDetailDto(
        long id,
        Long userId,
        Long organizationId,
        String name,
        String email,
        String phone,
        String cpf,
        String address,
        String city,
        String state,
        String notes,
        Long assignedSalespersonUserId,
        String createdAt,
        String updatedAt) {
    public CustomerDetailDto {
        id = DtoValidation.requirePositive(id, "Customer ID");
        userId = DtoValidation.optionalPositive(userId, "Customer user ID");
        organizationId = DtoValidation.optionalPositive(
                organizationId,
                "Customer organization ID");
        name = DtoValidation.requireText(name, "Customer name", 200);
        email = DtoValidation.optionalText(email, "Customer email", 320);
        phone = DtoValidation.optionalText(phone, "Customer phone", 20);
        cpf = DtoValidation.optionalText(cpf, "Customer CPF", 14);
        address = DtoValidation.optionalText(address, "Customer address", 2_000);
        city = DtoValidation.optionalText(city, "Customer city", 100);
        state = DtoValidation.optionalText(state, "Customer state", 2);
        notes = DtoValidation.optionalText(notes, "Customer notes", 4_000);
        assignedSalespersonUserId = DtoValidation.optionalPositive(
                assignedSalespersonUserId,
                "Assigned salesperson ID");
        createdAt = DtoValidation.requireInstant(
                createdAt,
                "Customer creation time");
        updatedAt = DtoValidation.requireInstant(
                updatedAt,
                "Customer update time");
    }
}
