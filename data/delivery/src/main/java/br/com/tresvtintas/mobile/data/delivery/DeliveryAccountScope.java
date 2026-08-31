package br.com.tresvtintas.mobile.data.delivery;

public record DeliveryAccountScope(
        long userId,
        String authorizationRevision) {
    public DeliveryAccountScope {
        if (userId < 1
                || authorizationRevision == null
                || !authorizationRevision.matches("^[0-9a-f]{64}$")) {
            throw new IllegalArgumentException("Delivery account scope is invalid.");
        }
    }
}
