package br.com.tresvtintas.mobile.core.network.problem;

import java.net.URI;
import java.util.UUID;

/**
 * RFC 9457-compatible error body with stable 3V error code and request correlation.
 */
public record ProblemDetails(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        MobileProblemCode code,
        String requestId) {

    public ProblemDetails {
        requireUri(type);
        requireLength(title, "Problem title", 120);
        if (status < 400 || status > 599) {
            throw new IllegalArgumentException("Problem status is outside the error range.");
        }
        requireLength(detail, "Problem detail", 300);
        if (instance == null || !instance.startsWith("urn:3v:request:")) {
            throw new IllegalArgumentException("Problem instance is invalid.");
        }
        if (code == null) {
            throw new IllegalArgumentException("Problem code is required.");
        }
        requireUuid(requestId);
    }

    private static void requireUri(String value) {
        try {
            if (value == null || !URI.create(value).isAbsolute()) {
                throw new IllegalArgumentException("Problem type must be an absolute URI.");
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Problem type must be an absolute URI.", exception);
        }
    }

    private static void requireLength(String value, String fieldName, int maximum) {
        if (value == null || value.isBlank() || value.length() > maximum) {
            throw new IllegalArgumentException(fieldName + " is invalid.");
        }
    }

    private static void requireUuid(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Problem request ID is invalid.");
        }
        try {
            UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Problem request ID is invalid.", exception);
        }
    }
}
