package br.com.tresvtintas.mobile.core.attendance;

import java.util.Objects;
import java.util.Optional;

public final class AttendanceException extends Exception {
    private static final long serialVersionUID = 1L;
    private final AttendanceFailureKind kind;
    private final String requestId;

    public AttendanceException(
            AttendanceFailureKind kind,
            String message) {
        this(kind, message, Optional.empty(), null);
    }

    public AttendanceException(
            AttendanceFailureKind kind,
            String message,
            Throwable cause) {
        this(kind, message, Optional.empty(), cause);
    }

    public AttendanceException(
            AttendanceFailureKind kind,
            String message,
            Optional<String> requestId,
            Throwable cause) {
        super(message, cause);
        this.kind = Objects.requireNonNull(
                kind,
                "Attendance failure kind is required.");
        this.requestId = Objects.requireNonNull(
                        requestId,
                        "Attendance request ID is required.")
                .orElse(null);
    }

    public AttendanceFailureKind kind() {
        return kind;
    }

    public Optional<String> requestId() {
        return Optional.ofNullable(requestId);
    }
}
