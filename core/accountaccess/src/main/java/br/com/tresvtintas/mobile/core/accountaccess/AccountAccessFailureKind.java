package br.com.tresvtintas.mobile.core.accountaccess;

public enum AccountAccessFailureKind {
    NETWORK,
    AUTH_REJECTED,
    FORBIDDEN,
    NOT_FOUND,
    CONFLICT,
    INVALID_REQUEST,
    RATE_LIMITED,
    UPDATE_REQUIRED,
    SERVICE_UNAVAILABLE,
    PROTOCOL,
    ACCESS_REVOKED
}
