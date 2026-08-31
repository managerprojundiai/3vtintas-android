package br.com.tresvtintas.mobile.core.accountaccess;

import java.util.Optional;

public final class AccountAccessException extends Exception {
    private static final long serialVersionUID = 1L;
    private final AccountAccessFailureKind kind;
    private final String requestId;

    public AccountAccessException(
            AccountAccessFailureKind kind,
            String message) {
        this(kind, message, null, null);
    }

    public AccountAccessException(
            AccountAccessFailureKind kind,
            String message,
            Throwable cause) {
        this(kind, message, null, cause);
    }

    public AccountAccessException(
            AccountAccessFailureKind kind,
            String message,
            String requestId,
            Throwable cause) {
        super(message, cause);
        if (kind == null || message == null || message.isBlank()) {
            throw new IllegalArgumentException(
                    "Account access failure is invalid.");
        }
        this.kind = kind;
        this.requestId = requestId;
    }

    public AccountAccessFailureKind kind() {
        return kind;
    }

    public Optional<String> requestId() {
        return Optional.ofNullable(requestId);
    }
}
