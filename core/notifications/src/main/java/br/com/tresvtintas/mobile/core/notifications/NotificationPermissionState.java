package br.com.tresvtintas.mobile.core.notifications;

import java.util.Arrays;
import java.util.Optional;

public enum NotificationPermissionState {
    UNKNOWN("unknown"),
    GRANTED("granted"),
    DENIED("denied");

    private final String wireValue;

    NotificationPermissionState(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    public static Optional<NotificationPermissionState> fromWireValue(String value) {
        return Arrays.stream(values())
                .filter(candidate -> candidate.wireValue.equals(value))
                .findFirst();
    }
}
