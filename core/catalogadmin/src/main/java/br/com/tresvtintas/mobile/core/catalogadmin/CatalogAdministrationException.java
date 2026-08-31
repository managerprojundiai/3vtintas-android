package br.com.tresvtintas.mobile.core.catalogadmin;

import java.util.Objects;
import java.util.Optional;

public final class CatalogAdministrationException extends Exception {
    private static final long serialVersionUID = 1L;
    private final CatalogAdministrationFailureKind kind;
    private final String requestId;

    public CatalogAdministrationException(
            CatalogAdministrationFailureKind kind,
            String message) {
        this(kind, message, Optional.empty(), null);
    }

    public CatalogAdministrationException(
            CatalogAdministrationFailureKind kind,
            String message,
            Throwable cause) {
        this(kind, message, Optional.empty(), cause);
    }

    public CatalogAdministrationException(
            CatalogAdministrationFailureKind kind,
            String message,
            Optional<String> requestId,
            Throwable cause) {
        super(message, cause);
        this.kind = Objects.requireNonNull(kind, "Failure kind is required.");
        this.requestId = requestId == null ? null : requestId.orElse(null);
    }

    public CatalogAdministrationFailureKind kind() {
        return kind;
    }

    public Optional<String> requestId() {
        return Optional.ofNullable(requestId);
    }
}
