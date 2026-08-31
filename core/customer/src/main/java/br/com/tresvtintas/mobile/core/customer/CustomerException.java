package br.com.tresvtintas.mobile.core.customer;

import java.util.Optional;

public final class CustomerException extends Exception {
    private static final long serialVersionUID = 1L;
    private final CustomerFailureKind kind;
    private final String requestId;

    public CustomerException(CustomerFailureKind kind, String message) {
        this(kind, message, null, null);
    }

    public CustomerException(
            CustomerFailureKind kind,
            String message,
            Throwable cause) {
        this(kind, message, null, cause);
    }

    public CustomerException(
            CustomerFailureKind kind,
            String message,
            String requestId,
            Throwable cause) {
        super(message, cause);
        if (kind == null) {
            throw new IllegalArgumentException("Customer failure kind is required.");
        }
        this.kind = kind;
        this.requestId = requestId;
    }

    public CustomerFailureKind kind() {
        return kind;
    }

    public Optional<String> requestId() {
        return Optional.ofNullable(requestId);
    }
}
