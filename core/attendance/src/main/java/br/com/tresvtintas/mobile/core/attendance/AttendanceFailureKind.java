package br.com.tresvtintas.mobile.core.attendance;

public enum AttendanceFailureKind {
    ACCESS_REVOKED,
    AUTH_REJECTED,
    CONFLICT,
    FORBIDDEN,
    IDEMPOTENCY_IN_PROGRESS,
    IDEMPOTENCY_KEY_REUSED,
    INVALID_REQUEST,
    NETWORK,
    NOT_FOUND,
    PROTOCOL,
    RATE_LIMITED,
    SERVICE_UNAVAILABLE,
    UPDATE_REQUIRED
}
