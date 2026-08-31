package br.com.tresvtintas.mobile.core.appointment;

import java.util.Optional;

public final class AppointmentException extends Exception {
    private static final long serialVersionUID = 1L;
    private final AppointmentFailureKind kind;
    private final String requestId;

    public AppointmentException(
            AppointmentFailureKind kind,
            String message) {
        this(kind, message, null, null);
    }

    public AppointmentException(
            AppointmentFailureKind kind,
            String message,
            Throwable cause) {
        this(kind, message, null, cause);
    }

    public AppointmentException(
            AppointmentFailureKind kind,
            String message,
            String requestId,
            Throwable cause) {
        super(message, cause);
        this.kind = java.util.Objects.requireNonNull(
                kind,
                "Appointment failure kind is required.");
        this.requestId = requestId;
    }

    public AppointmentFailureKind kind() {
        return kind;
    }

    public Optional<String> requestId() {
        return Optional.ofNullable(requestId);
    }
}
