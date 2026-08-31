package br.com.tresvtintas.mobile.core.dashboard;

import java.util.Optional;

public final class DashboardException extends Exception {
    private static final long serialVersionUID = 1L;
    private final DashboardFailureKind kind;
    private final String requestId;

    public DashboardException(
            DashboardFailureKind kind,
            String message) {
        this(kind, message, null, null);
    }

    public DashboardException(
            DashboardFailureKind kind,
            String message,
            Throwable cause) {
        this(kind, message, null, cause);
    }

    public DashboardException(
            DashboardFailureKind kind,
            String message,
            String requestId,
            Throwable cause) {
        super(message, cause);
        if (kind == null || message == null || message.isBlank()) {
            throw new IllegalArgumentException(
                    "Dashboard failure is invalid.");
        }
        this.kind = kind;
        this.requestId = requestId;
    }

    public DashboardFailureKind kind() {
        return kind;
    }

    public Optional<String> requestId() {
        return Optional.ofNullable(requestId);
    }
}
