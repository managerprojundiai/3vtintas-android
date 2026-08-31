package br.com.tresvtintas.mobile.core.commission;

import java.util.Optional;

public final class CommissionException extends Exception {
    private static final long serialVersionUID = 1L;
    private final CommissionFailureKind kind;
    private final String requestId;

    public CommissionException(
            CommissionFailureKind kind,
            String message) {
        this(kind, message, null, null);
    }

    public CommissionException(
            CommissionFailureKind kind,
            String message,
            Throwable cause) {
        this(kind, message, null, cause);
    }

    public CommissionException(
            CommissionFailureKind kind,
            String message,
            String requestId,
            Throwable cause) {
        super(message, cause);
        if (kind == null || message == null || message.isBlank()) {
            throw new IllegalArgumentException("Commission failure is invalid.");
        }
        this.kind = kind;
        this.requestId = requestId;
    }

    public CommissionFailureKind kind() {
        return kind;
    }

    public Optional<String> requestId() {
        return Optional.ofNullable(requestId);
    }
}
