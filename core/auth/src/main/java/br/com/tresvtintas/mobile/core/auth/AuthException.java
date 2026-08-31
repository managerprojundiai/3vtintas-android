package br.com.tresvtintas.mobile.core.auth;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

/**
 * Sanitized authentication failure. Credential values must never be included in its message.
 */
public final class AuthException extends IOException {
    private static final long serialVersionUID = 1L;
    private final AuthFailureKind kind;
    private final String requestId;

    public AuthException(AuthFailureKind kind, String message) {
        this(kind, message, null, null);
    }

    public AuthException(AuthFailureKind kind, String message, Throwable cause) {
        this(kind, message, null, cause);
    }

    public AuthException(
            AuthFailureKind kind,
            String message,
            String requestId,
            Throwable cause) {
        super(message, cause);
        this.kind = Objects.requireNonNull(kind, "Failure kind is required.");
        this.requestId = requestId;
    }

    public AuthFailureKind kind() {
        return kind;
    }

    public Optional<String> requestId() {
        return Optional.ofNullable(requestId);
    }
}
