package br.com.tresvtintas.mobile.core.agent;

import java.util.Objects;
import java.util.Optional;

public final class AgentException extends Exception {
    private static final long serialVersionUID = 1L;
    private final AgentFailureKind kind;
    private final String requestId;

    public AgentException(AgentFailureKind kind, String message) {
        this(kind, message, Optional.empty(), null);
    }

    public AgentException(
            AgentFailureKind kind,
            String message,
            Throwable cause) {
        this(kind, message, Optional.empty(), cause);
    }

    public AgentException(
            AgentFailureKind kind,
            String message,
            Optional<String> requestId,
            Throwable cause) {
        super(message, cause);
        this.kind = Objects.requireNonNull(
                kind,
                "Agent failure kind is required.");
        this.requestId = Objects.requireNonNull(
                requestId,
                "Agent request ID is required.")
                .orElse(null);
    }

    public AgentFailureKind kind() {
        return kind;
    }

    public Optional<String> requestId() {
        return Optional.ofNullable(requestId);
    }
}
