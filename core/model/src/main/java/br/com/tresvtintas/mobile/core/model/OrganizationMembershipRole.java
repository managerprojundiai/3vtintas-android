package br.com.tresvtintas.mobile.core.model;

import java.util.Optional;

public enum OrganizationMembershipRole {
    OWNER("owner"),
    MANAGER("manager"),
    SALESPERSON("salesperson"),
    DELIVERY_DRIVER("delivery_driver"),
    PAINTER("painter"),
    MEMBER("member");

    private final String wireValue;

    OrganizationMembershipRole(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    public static Optional<OrganizationMembershipRole> fromWireValue(String value) {
        if (value == null) {
            return Optional.empty();
        }
        for (OrganizationMembershipRole role : values()) {
            if (role.wireValue.equals(value)) {
                return Optional.of(role);
            }
        }
        return Optional.empty();
    }
}
