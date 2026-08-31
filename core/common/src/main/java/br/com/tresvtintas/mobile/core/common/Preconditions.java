package br.com.tresvtintas.mobile.core.common;

/**
 * Small dependency-free argument guards shared by business modules.
 */
public final class Preconditions {
    private Preconditions() {
        throw new AssertionError("No instances.");
    }

    public static String requireNonBlank(String value, String parameterName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(parameterName + " must not be blank.");
        }
        return value;
    }
}
