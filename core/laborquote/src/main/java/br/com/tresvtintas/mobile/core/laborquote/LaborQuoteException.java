package br.com.tresvtintas.mobile.core.laborquote;

import java.util.Optional;

public final class LaborQuoteException extends Exception {
    private static final long serialVersionUID = 1L;
    private final LaborQuoteFailureKind kind;
    private final String requestId;

    public LaborQuoteException(LaborQuoteFailureKind kind, String message) {
        this(kind, message, null, null);
    }

    public LaborQuoteException(
            LaborQuoteFailureKind kind,
            String message,
            Throwable cause) {
        this(kind, message, null, cause);
    }

    public LaborQuoteException(
            LaborQuoteFailureKind kind,
            String message,
            String requestId,
            Throwable cause) {
        super(message, cause);
        if (kind == null) {
            throw new IllegalArgumentException("Labor quote failure kind is required.");
        }
        this.kind = kind;
        this.requestId = requestId;
    }

    public LaborQuoteFailureKind kind() {
        return kind;
    }

    public Optional<String> requestId() {
        return Optional.ofNullable(requestId);
    }
}
