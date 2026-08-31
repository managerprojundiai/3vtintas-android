package br.com.tresvtintas.mobile.core.network.dto;

public record CustomerUpdateRequest(
        String name,
        String email,
        String phone,
        String cpf,
        String address,
        String city,
        String state,
        String notes) {
    public CustomerUpdateRequest {
        CustomerCreateRequest.validateCustomerFields(
                name,
                email,
                phone,
                cpf,
                address,
                city,
                state,
                notes);
    }
}
