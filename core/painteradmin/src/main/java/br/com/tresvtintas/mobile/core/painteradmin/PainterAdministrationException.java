package br.com.tresvtintas.mobile.core.painteradmin;

import java.util.Optional;

public final class PainterAdministrationException extends Exception {
    private static final long serialVersionUID = 1L;
    private final PainterAdministrationFailureKind kind;
    private final String requestId;

    public PainterAdministrationException(
            PainterAdministrationFailureKind kind,
            String message) {
        this(kind, message, Optional.empty(), null);
    }

    public PainterAdministrationException(
            PainterAdministrationFailureKind kind,
            String message,
            Throwable cause) {
        this(kind, message, Optional.empty(), cause);
    }

    public PainterAdministrationException(
            PainterAdministrationFailureKind kind,
            String message,
            Optional<String> requestId,
            Throwable cause) {
        super(message, cause);
        this.kind = kind;
        this.requestId = requestId == null
                ? null
                : requestId.orElse(null);
    }

    public PainterAdministrationFailureKind kind() {
        return kind;
    }

    public Optional<String> requestId() {
        return Optional.ofNullable(requestId);
    }
}
