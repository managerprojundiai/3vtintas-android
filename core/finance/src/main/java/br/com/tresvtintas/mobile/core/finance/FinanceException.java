package br.com.tresvtintas.mobile.core.finance;

import java.util.Optional;

public final class FinanceException extends Exception {
    private static final long serialVersionUID = 1L;
    private final FinanceFailureKind kind;
    private final String requestId;

    public FinanceException(FinanceFailureKind kind, String message) {
        this(kind, message, null, null);
    }

    public FinanceException(
            FinanceFailureKind kind,
            String message,
            Throwable cause) {
        this(kind, message, null, cause);
    }

    public FinanceException(
            FinanceFailureKind kind,
            String message,
            String requestId,
            Throwable cause) {
        super(message, cause);
        if (kind == null || message == null || message.isBlank()) {
            throw new IllegalArgumentException("Finance failure is invalid.");
        }
        this.kind = kind;
        this.requestId = requestId;
    }

    public FinanceFailureKind kind() {
        return kind;
    }

    public Optional<String> requestId() {
        return Optional.ofNullable(requestId);
    }
}
