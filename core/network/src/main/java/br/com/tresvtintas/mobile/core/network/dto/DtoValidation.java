package br.com.tresvtintas.mobile.core.network.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import java.util.regex.Pattern;

final class DtoValidation {
    private static final int MINIMUM_ID = 1;
    static final Pattern REFRESH_TOKEN = Pattern.compile("^3vr1_[A-Za-z0-9_-]{43}$");
    static final Pattern NONCE = Pattern.compile("^3vn1_[A-Za-z0-9_-]{43}$");
    static final Pattern SHA_256 = Pattern.compile("^[0-9a-f]{64}$");

    private DtoValidation() {
    }

    static String requireText(String value, String fieldName, int maximumLength) {
        if (value == null || value.isBlank() || value.length() > maximumLength) {
            throw new IllegalArgumentException(fieldName + " is invalid.");
        }
        return value;
    }

    static String requireUuid(String value, String fieldName) {
        requireText(value, fieldName, 36);
        try {
            return UUID.fromString(value).toString();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(fieldName + " is not a UUID.", exception);
        }
    }

    static String requireInstant(String value, String fieldName) {
        requireText(value, fieldName, 40);
        try {
            Instant.parse(value);
            return value;
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(fieldName + " is not an ISO instant.", exception);
        }
    }

    static String requireSha256(String value, String fieldName) {
        requireText(value, fieldName, 64);
        if (!SHA_256.matcher(value).matches()) {
            throw new IllegalArgumentException(fieldName + " is not a SHA-256 value.");
        }
        return value;
    }

    static String requireDate(String value, String fieldName) {
        requireText(value, fieldName, 10);
        try {
            if (!LocalDate.parse(value).toString().equals(value)) {
                throw new IllegalArgumentException(fieldName + " is not an ISO date.");
            }
            return value;
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(fieldName + " is not an ISO date.", exception);
        }
    }

    static long requirePositive(long value, String fieldName) {
        if (value < MINIMUM_ID) {
            throw new IllegalArgumentException(fieldName + " is invalid.");
        }
        return value;
    }

    static Long optionalPositive(Long value, String fieldName) {
        return value == null ? null : requirePositive(value, fieldName);
    }

    static String optionalText(
            String value,
            String fieldName,
            int maximumLength) {
        if (value == null) {
            return null;
        }
        if (value.length() > maximumLength) {
            throw new IllegalArgumentException(fieldName + " is invalid.");
        }
        return value;
    }
}
