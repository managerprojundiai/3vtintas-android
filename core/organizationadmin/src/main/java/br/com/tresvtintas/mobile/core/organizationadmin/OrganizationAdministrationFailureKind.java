package br.com.tresvtintas.mobile.core.organizationadmin;

public enum OrganizationAdministrationFailureKind {
    AUTH_REJECTED,
    FORBIDDEN,
    NOT_FOUND,
    CONFLICT,
    LAST_ACTIVE,
    SLUG_CONFLICT,
    INVALID_REQUEST,
    IDEMPOTENCY_IN_PROGRESS,
    IDEMPOTENCY_KEY_REUSED,
    UPDATE_REQUIRED,
    RATE_LIMITED,
    SERVICE_UNAVAILABLE,
    NETWORK,
    PROTOCOL,
    ACCESS_REVOKED
}
