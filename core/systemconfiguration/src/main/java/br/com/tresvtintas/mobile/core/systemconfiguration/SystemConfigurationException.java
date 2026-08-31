package br.com.tresvtintas.mobile.core.systemconfiguration;

import java.util.Objects;
import java.util.Optional;

public final class SystemConfigurationException extends Exception {
    private static final long serialVersionUID = 1L;
    private final SystemConfigurationFailureKind kind;
    private final String requestId;

    public SystemConfigurationException(
            SystemConfigurationFailureKind kind,
            String message) {
        this(kind, message, Optional.empty(), null);
    }

    public SystemConfigurationException(
            SystemConfigurationFailureKind kind,
            String message,
            Throwable cause) {
        this(kind, message, Optional.empty(), cause);
    }

    public SystemConfigurationException(
            SystemConfigurationFailureKind kind,
            String message,
            Optional<String> requestId,
            Throwable cause) {
        super(message, cause);
        this.kind = Objects.requireNonNull(kind, "Failure kind is required.");
        this.requestId = requestId == null ? null : requestId.orElse(null);
    }

    public SystemConfigurationFailureKind kind() {
        return kind;
    }

    public Optional<String> requestId() {
        return Optional.ofNullable(requestId);
    }
}
