package br.com.tresvtintas.mobile.core.dashboard;

import java.util.Locale;
import java.util.Optional;

public enum DashboardVisibility {
    SELF,
    TEAM,
    ALL;

    public static Optional<DashboardVisibility> fromWireValue(String value) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(value.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
