package br.com.tresvtintas.mobile.core.bootstrap;

import java.util.Optional;

public final class BootstrapException extends Exception {
    private static final long serialVersionUID = 1L;
    private final BootstrapFailureKind kind;
    private final String requestId;

    public BootstrapException(BootstrapFailureKind kind, String message) {
        this(kind, message, null, null);
    }

    public BootstrapException(
            BootstrapFailureKind kind,
            String message,
            Throwable cause) {
        this(kind, message, null, cause);
    }

    public BootstrapException(
            BootstrapFailureKind kind,
            String message,
            String requestId,
            Throwable cause) {
        super(message, cause);
        if (kind == null) {
            throw new IllegalArgumentException("Bootstrap failure kind is required.");
        }
        this.kind = kind;
        this.requestId = requestId;
    }

    public BootstrapFailureKind kind() {
        return kind;
    }

    public Optional<String> requestId() {
        return Optional.ofNullable(requestId);
    }
}
