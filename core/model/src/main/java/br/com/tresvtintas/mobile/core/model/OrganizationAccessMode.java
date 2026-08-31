package br.com.tresvtintas.mobile.core.model;

import java.util.Optional;

public enum OrganizationAccessMode {
    ALL("all"),
    ASSIGNED("assigned"),
    NONE("none");

    private final String wireValue;

    OrganizationAccessMode(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    public static Optional<OrganizationAccessMode> fromWireValue(String value) {
        if (value == null) {
            return Optional.empty();
        }
        for (OrganizationAccessMode mode : values()) {
            if (mode.wireValue.equals(value)) {
                return Optional.of(mode);
            }
        }
        return Optional.empty();
    }
}
