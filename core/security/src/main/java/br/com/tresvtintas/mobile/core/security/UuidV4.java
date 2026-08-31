package br.com.tresvtintas.mobile.core.security;

import java.util.Locale;
import java.util.UUID;

final class UuidV4 {
    private UuidV4() {
    }

    static String require(String value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        try {
            UUID parsed = UUID.fromString(value);
            String canonical = parsed.toString().toLowerCase(Locale.ROOT);
            if (parsed.version() != 4 || !canonical.equals(value.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException(fieldName + " must be a canonical UUID v4.");
            }
            return canonical;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(fieldName + " must be a canonical UUID v4.", exception);
        }
    }
}
