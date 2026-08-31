package br.com.tresvtintas.mobile.feature.painteradmin;

import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationFailureKind;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationStatus;

final class PainterAdministrationText {
    private PainterAdministrationText() {
        throw new AssertionError("No instances.");
    }

    static int failure(PainterAdministrationFailureKind kind) {
        return switch (kind) {
            case ACCESS_REVOKED, AUTH_REJECTED, FORBIDDEN ->
                    R.string.painter_admin_error_access;
            case CONFLICT -> R.string.painter_admin_error_conflict;
            case IDEMPOTENCY_IN_PROGRESS ->
                    R.string.painter_admin_error_in_progress;
            case IDEMPOTENCY_KEY_REUSED, INVALID_REQUEST ->
                    R.string.painter_admin_error_invalid;
            case NETWORK -> R.string.painter_admin_error_network;
            case NOT_FOUND -> R.string.painter_admin_error_not_found;
            case RATE_LIMITED -> R.string.painter_admin_error_rate_limited;
            case SERVICE_UNAVAILABLE ->
                    R.string.painter_admin_error_service;
            case UPDATE_REQUIRED -> R.string.painter_admin_error_update;
            case PROTOCOL -> R.string.painter_admin_error_protocol;
        };
    }

    static int status(PainterAdministrationStatus status) {
        return switch (status) {
            case ACTIVE -> R.string.painter_admin_status_active;
            case BLOCKED -> R.string.painter_admin_status_blocked;
            case PENDING -> R.string.painter_admin_status_pending;
        };
    }
}
