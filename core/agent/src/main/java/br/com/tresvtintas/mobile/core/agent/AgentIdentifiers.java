package br.com.tresvtintas.mobile.core.agent;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

final class AgentIdentifiers {
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-"
                    + "[1-8][0-9a-fA-F]{3}-"
                    + "[89abAB][0-9a-fA-F]{3}-"
                    + "[0-9a-fA-F]{12}$");

    private AgentIdentifiers() {
        throw new AssertionError("No instances.");
    }

    static String requireUuid(String value, String message) {
        if (value == null || !UUID_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(message);
        }
        try {
            return UUID.fromString(value)
                    .toString()
                    .toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException(message, failure);
        }
    }
}
