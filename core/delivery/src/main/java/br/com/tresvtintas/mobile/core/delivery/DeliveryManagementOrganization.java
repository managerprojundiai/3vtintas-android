package br.com.tresvtintas.mobile.core.delivery;

public record DeliveryManagementOrganization(long id, String name) {
    public DeliveryManagementOrganization {
        if (id < 1 || name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Delivery management organization is invalid.");
        }
    }
}
