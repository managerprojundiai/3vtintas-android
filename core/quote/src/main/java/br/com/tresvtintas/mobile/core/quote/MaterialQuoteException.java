package br.com.tresvtintas.mobile.core.quote;

import java.util.Optional;

public final class MaterialQuoteException extends Exception {
    private static final long serialVersionUID = 1L;
    private final MaterialQuoteFailureKind kind;
    private final String requestId;

    public MaterialQuoteException(
            MaterialQuoteFailureKind kind,
            String message) {
        this(kind, message, null, null);
    }

    public MaterialQuoteException(
            MaterialQuoteFailureKind kind,
            String message,
            Throwable cause) {
        this(kind, message, null, cause);
    }

    public MaterialQuoteException(
            MaterialQuoteFailureKind kind,
            String message,
            String requestId,
            Throwable cause) {
        super(message, cause);
        if (kind == null) {
            throw new IllegalArgumentException("Quote failure kind is required.");
        }
        this.kind = kind;
        this.requestId = requestId;
    }

    public MaterialQuoteFailureKind kind() {
        return kind;
    }

    public Optional<String> requestId() {
        return Optional.ofNullable(requestId);
    }
}
