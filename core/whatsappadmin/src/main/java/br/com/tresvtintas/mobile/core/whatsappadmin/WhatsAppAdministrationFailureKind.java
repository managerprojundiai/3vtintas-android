package br.com.tresvtintas.mobile.core.whatsappadmin;

public enum WhatsAppAdministrationFailureKind {
    AUTH_REJECTED,
    FORBIDDEN,
    INVALID_REQUEST,
    CONFLICT,
    IDEMPOTENCY_IN_PROGRESS,
    IDEMPOTENCY_KEY_REUSED,
    RATE_LIMITED,
    SERVICE_UNAVAILABLE,
    UPDATE_REQUIRED,
    NETWORK,
    PROTOCOL,
    ACCESS_REVOKED
}
