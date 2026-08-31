package br.com.tresvtintas.mobile.app;

import java.util.Locale;

/**
 * Presentation-only state for the first executable checkpoint.
 */
public final class FoundationStatus {
    private FoundationStatus() {
        throw new AssertionError("No instances.");
    }

    public static String formatEnvironment(String environment) {
        if (environment == null || environment.isBlank()) {
            return "DESCONHECIDO";
        }
        return environment.trim().toUpperCase(Locale.ROOT);
    }
}
