package br.com.tresvtintas.mobile.core.network.dto;

public record CustomerCreateRequest(
        Long organizationId,
        String name,
        String email,
        String phone,
        String cpf,
        String address,
        String city,
        String state,
        String notes) {
    public CustomerCreateRequest {
        organizationId = DtoValidation.optionalPositive(
                organizationId,
                "Customer organization ID");
        validateCustomerFields(
                name,
                email,
                phone,
                cpf,
                address,
                city,
                state,
                notes);
    }

    static void validateCustomerFields(
            String name,
            String email,
            String phone,
            String cpf,
            String address,
            String city,
            String state,
            String notes) {
        DtoValidation.requireText(name, "Customer name", 200);
        DtoValidation.optionalText(email, "Customer email", 320);
        DtoValidation.optionalText(phone, "Customer phone", 20);
        DtoValidation.optionalText(cpf, "Customer CPF", 14);
        DtoValidation.optionalText(address, "Customer address", 2_000);
        DtoValidation.optionalText(city, "Customer city", 100);
        DtoValidation.optionalText(state, "Customer state", 2);
        DtoValidation.optionalText(notes, "Customer notes", 4_000);
    }
}
