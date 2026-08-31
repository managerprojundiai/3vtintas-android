package br.com.tresvtintas.mobile.core.customer;

public record CustomerMutationResult(
        long customerId,
        boolean changed,
        boolean replayed) {
    public CustomerMutationResult {
        customerId = CustomerValues.positiveId(customerId, "Customer ID");
    }
}
