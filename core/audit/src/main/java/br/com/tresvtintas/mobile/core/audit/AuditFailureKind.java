package br.com.tresvtintas.mobile.core.audit;

public enum AuditFailureKind {
    ACCESS_REVOKED,
    AUTH_REJECTED,
    FORBIDDEN,
    INVALID_REQUEST,
    NETWORK,
    PROTOCOL,
    RATE_LIMITED,
    SERVICE_UNAVAILABLE,
    UPDATE_REQUIRED
}
