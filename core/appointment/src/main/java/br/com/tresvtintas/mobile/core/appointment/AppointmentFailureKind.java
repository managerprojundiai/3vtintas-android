package br.com.tresvtintas.mobile.core.appointment;

public enum AppointmentFailureKind {
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
