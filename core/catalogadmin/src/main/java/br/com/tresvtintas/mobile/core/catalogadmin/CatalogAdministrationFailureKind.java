package br.com.tresvtintas.mobile.core.catalogadmin;

public enum CatalogAdministrationFailureKind {
    AUTH_REJECTED,
    FORBIDDEN,
    NOT_FOUND,
    CONFLICT,
    INVALID_REQUEST,
    IDEMPOTENCY_IN_PROGRESS,
    IDEMPOTENCY_KEY_REUSED,
    PAYLOAD_TOO_LARGE,
    UPDATE_REQUIRED,
    RATE_LIMITED,
    SERVICE_UNAVAILABLE,
    NETWORK,
    PROTOCOL,
    ACCESS_REVOKED
}
