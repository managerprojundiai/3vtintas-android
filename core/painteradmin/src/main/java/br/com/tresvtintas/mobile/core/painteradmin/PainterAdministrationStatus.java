package br.com.tresvtintas.mobile.core.painteradmin;

import java.util.Optional;

public enum PainterAdministrationStatus {
    PENDING("pending"),
    ACTIVE("active"),
    BLOCKED("blocked");

    private final String wireValue;

    PainterAdministrationStatus(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    public static Optional<PainterAdministrationStatus> fromWireValue(
            String value) {
        for (PainterAdministrationStatus status : values()) {
            if (status.wireValue.equals(value)) {
                return Optional.of(status);
            }
        }
        return Optional.empty();
    }
}
