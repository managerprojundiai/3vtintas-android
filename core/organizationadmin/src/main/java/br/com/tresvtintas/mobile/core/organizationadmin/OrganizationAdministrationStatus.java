package br.com.tresvtintas.mobile.core.organizationadmin;

import java.util.Optional;

public enum OrganizationAdministrationStatus {
    PENDING("pending"),
    ACTIVE("active"),
    BLOCKED("blocked");

    private final String wireValue;

    OrganizationAdministrationStatus(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    public static Optional<OrganizationAdministrationStatus> fromWireValue(
            String value) {
        for (OrganizationAdministrationStatus status : values()) {
            if (status.wireValue.equals(value)) {
                return Optional.of(status);
            }
        }
        return Optional.empty();
    }
}
