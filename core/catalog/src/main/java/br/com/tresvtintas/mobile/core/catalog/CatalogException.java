package br.com.tresvtintas.mobile.core.catalog;

import java.util.Optional;

public final class CatalogException extends Exception {
    private static final long serialVersionUID = 1L;
    private final CatalogFailureKind kind;
    private final String requestId;

    public CatalogException(CatalogFailureKind kind, String message) {
        this(kind, message, null, null);
    }

    public CatalogException(
            CatalogFailureKind kind,
            String message,
            Throwable cause) {
        this(kind, message, null, cause);
    }

    public CatalogException(
            CatalogFailureKind kind,
            String message,
            String requestId,
            Throwable cause) {
        super(message, cause);
        if (kind == null) {
            throw new IllegalArgumentException("Catalog failure kind is required.");
        }
        this.kind = kind;
        this.requestId = requestId;
    }

    public CatalogFailureKind kind() {
        return kind;
    }

    public Optional<String> requestId() {
        return Optional.ofNullable(requestId);
    }
}
