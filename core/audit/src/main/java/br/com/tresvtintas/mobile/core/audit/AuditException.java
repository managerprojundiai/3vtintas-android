package br.com.tresvtintas.mobile.core.audit;

import java.util.Optional;

public final class AuditException extends Exception {
    private static final long serialVersionUID = 1L;
    private final AuditFailureKind kind;
    private final String requestId;

    public AuditException(AuditFailureKind kind, String message) {
        this(kind, message, Optional.empty(), null);
    }

    public AuditException(AuditFailureKind kind, String message, Throwable cause) {
        this(kind, message, Optional.empty(), cause);
    }

    public AuditException(
            AuditFailureKind kind,
            String message,
            Optional<String> requestId,
            Throwable cause) {
        super(message, cause);
        this.kind = kind;
        this.requestId = requestId == null ? null : requestId.orElse(null);
    }

    public AuditFailureKind kind() {
        return kind;
    }

    public Optional<String> requestId() {
        return Optional.ofNullable(requestId);
    }
}
