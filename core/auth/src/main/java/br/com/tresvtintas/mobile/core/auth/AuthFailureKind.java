package br.com.tresvtintas.mobile.core.auth;

/**
 * Stable client-side classes used to present recoverable authentication failures.
 */
public enum AuthFailureKind {
    AUTH_REJECTED,
    CANCELED,
    CONFIGURATION,
    NETWORK,
    NO_CREDENTIAL,
    PROTOCOL,
    RATE_LIMITED,
    SERVICE_UNAVAILABLE,
    STORAGE,
    UPDATE_REQUIRED
}
