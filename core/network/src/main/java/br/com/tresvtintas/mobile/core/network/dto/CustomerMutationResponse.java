package br.com.tresvtintas.mobile.core.network.dto;

public record CustomerMutationResponse(
        long customerId,
        boolean changed) {
    public CustomerMutationResponse {
        customerId = DtoValidation.requirePositive(
                customerId,
                "Customer ID");
    }
}
