package br.com.tresvtintas.mobile.core.delivery;

import java.util.Optional;

public final class DeliveryException extends Exception {
    private static final long serialVersionUID = 1L;
    private final DeliveryFailureKind kind;
    private final String requestId;

    public DeliveryException(DeliveryFailureKind kind, String message) {
        this(kind, message, Optional.empty(), null);
    }

    public DeliveryException(
            DeliveryFailureKind kind,
            String message,
            Throwable cause) {
        this(kind, message, Optional.empty(), cause);
    }

    public DeliveryException(
            DeliveryFailureKind kind,
            String message,
            Optional<String> requestId,
            Throwable cause) {
        super(message, cause);
        if (kind == null) {
            throw new IllegalArgumentException("Delivery failure kind is required.");
        }
        this.kind = kind;
        this.requestId = requestId == null
                ? null
                : requestId.orElse(null);
    }

    public DeliveryFailureKind kind() {
        return kind;
    }

    public Optional<String> requestId() {
        return Optional.ofNullable(requestId);
    }
}
