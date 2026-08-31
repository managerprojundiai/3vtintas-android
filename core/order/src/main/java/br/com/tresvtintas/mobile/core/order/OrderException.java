package br.com.tresvtintas.mobile.core.order;

import java.util.Optional;

public final class OrderException extends Exception {
    private static final long serialVersionUID = 1L;
    private final OrderFailureKind kind;
    private final String requestId;
    private final String userDetail;

    public OrderException(OrderFailureKind kind, String message) {
        this(kind, message, null, null, null);
    }

    public OrderException(OrderFailureKind kind, String message, Throwable cause) {
        this(kind, message, null, null, cause);
    }

    public OrderException(
            OrderFailureKind kind,
            String message,
            String requestId,
            Throwable cause) {
        this(kind, message, requestId, null, cause);
    }

    public OrderException(
            OrderFailureKind kind,
            String message,
            String requestId,
            String userDetail,
            Throwable cause) {
        super(message, cause);
        if (kind == null) {
            throw new IllegalArgumentException("Order failure kind is required.");
        }
        this.kind = kind;
        this.requestId = requestId;
        this.userDetail = userDetail;
    }

    public OrderFailureKind kind() {
        return kind;
    }

    public Optional<String> requestId() {
        return Optional.ofNullable(requestId);
    }

    public Optional<String> userDetail() {
        return Optional.ofNullable(userDetail);
    }
}
