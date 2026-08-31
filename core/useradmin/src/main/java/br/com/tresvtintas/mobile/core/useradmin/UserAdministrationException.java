package br.com.tresvtintas.mobile.core.useradmin;

import java.util.Optional;

public final class UserAdministrationException extends Exception {
    private static final long serialVersionUID = 1L;
    private final UserAdministrationFailureKind kind;
    private final String requestId;

    public UserAdministrationException(
            UserAdministrationFailureKind kind,
            String message) {
        this(kind, message, Optional.empty(), null);
    }

    public UserAdministrationException(
            UserAdministrationFailureKind kind,
            String message,
            Throwable cause) {
        this(kind, message, Optional.empty(), cause);
    }

    public UserAdministrationException(
            UserAdministrationFailureKind kind,
            String message,
            Optional<String> requestId,
            Throwable cause) {
        super(message, cause);
        this.kind = kind;
        this.requestId = requestId == null ? null : requestId.orElse(null);
    }

    public UserAdministrationFailureKind kind() {
        return kind;
    }

    public Optional<String> requestId() {
        return Optional.ofNullable(requestId);
    }
}
