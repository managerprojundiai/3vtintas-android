package br.com.tresvtintas.mobile.core.network.dto;

public record CustomerSummaryDto(
        long id,
        Long organizationId,
        String name,
        String email,
        String phone,
        String city,
        String state,
        Long assignedSalespersonUserId,
        String createdAt,
        String updatedAt) {
    public CustomerSummaryDto {
        id = DtoValidation.requirePositive(id, "Customer ID");
        organizationId = DtoValidation.optionalPositive(
                organizationId,
                "Customer organization ID");
        name = DtoValidation.requireText(name, "Customer name", 200);
        email = DtoValidation.optionalText(email, "Customer email", 320);
        phone = DtoValidation.optionalText(phone, "Customer phone", 20);
        city = DtoValidation.optionalText(city, "Customer city", 100);
        state = DtoValidation.optionalText(state, "Customer state", 2);
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
