package br.com.tresvtintas.mobile.core.model;

import java.util.Optional;

/**
 * Account role labels received from the mobile API. Authorization still depends on capabilities and
 * resource-level checks performed by the server.
 */
public enum AppRole {
    MASTER_ADMIN("master_admin"),
    MANAGER("manager"),
    SALESPERSON("salesperson"),
    DELIVERY_DRIVER("delivery_driver"),
    PAINTER("painter"),
    CUSTOMER("customer"),
    USER("user");

    private final String wireValue;

    AppRole(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    public static Optional<AppRole> fromWireValue(String value) {
        if (value == null) {
            return Optional.empty();
        }
        for (AppRole role : values()) {
            if (role.wireValue.equals(value)) {
                return Optional.of(role);
            }
        }
        return Optional.empty();
    }
}
