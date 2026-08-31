package br.com.tresvtintas.mobile.core.notifications;

import java.util.Optional;

public final class NotificationException extends Exception {
    private static final long serialVersionUID = 1L;
    private final NotificationFailureKind kind;
    private final String requestId;

    public NotificationException(
            NotificationFailureKind kind,
            String message) {
        this(kind, message, null, null);
    }

    public NotificationException(
            NotificationFailureKind kind,
            String message,
            Throwable cause) {
        this(kind, message, null, cause);
    }

    public NotificationException(
            NotificationFailureKind kind,
            String message,
            String requestId,
            Throwable cause) {
        super(message, cause);
        if (kind == null || message == null || message.isBlank()) {
            throw new IllegalArgumentException("Notification failure is invalid.");
        }
        this.kind = kind;
        this.requestId = requestId;
    }

    public NotificationFailureKind kind() {
        return kind;
    }

    public Optional<String> requestId() {
        return Optional.ofNullable(requestId);
    }
}
