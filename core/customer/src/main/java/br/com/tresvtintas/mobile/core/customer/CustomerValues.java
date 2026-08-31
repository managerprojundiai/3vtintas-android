package br.com.tresvtintas.mobile.core.customer;

import java.util.Optional;
import java.util.OptionalLong;

final class CustomerValues {
    private static final int MINIMUM_ID = 1;

    private CustomerValues() {
        throw new AssertionError("No instances.");
    }

    static long positiveId(long value, String fieldName) {
        if (value < MINIMUM_ID) {
            throw new IllegalArgumentException(fieldName + " must be positive.");
        }
        return value;
    }

    static OptionalLong optionalId(Long value, String fieldName) {
        return value == null
                ? OptionalLong.empty()
                : OptionalLong.of(positiveId(value, fieldName));
    }

    static String requiredText(String value, String fieldName, int maximumLength) {
        String normalized = normalize(value);
        if (normalized == null || normalized.length() > maximumLength) {
            throw new IllegalArgumentException(fieldName + " is invalid.");
        }
        return normalized;
    }

    static Optional<String> optionalText(
            String value,
            String fieldName,
            int maximumLength) {
        String normalized = normalize(value);
        if (normalized == null) {
            return Optional.empty();
        }
        if (normalized.length() > maximumLength) {
            throw new IllegalArgumentException(fieldName + " is invalid.");
        }
        return Optional.of(normalized);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isEmpty() ? null : normalized;
    }
}
