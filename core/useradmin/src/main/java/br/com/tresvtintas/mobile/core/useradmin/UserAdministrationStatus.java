package br.com.tresvtintas.mobile.core.useradmin;

import java.util.Optional;

public enum UserAdministrationStatus {
    ACTIVE("active"),
    BLOCKED("blocked");

    private final String wireValue;

    UserAdministrationStatus(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    public static Optional<UserAdministrationStatus> fromWireValue(String value) {
        for (UserAdministrationStatus status : values()) {
            if (status.wireValue.equals(value)) {
                return Optional.of(status);
            }
        }
        return Optional.empty();
    }
}
