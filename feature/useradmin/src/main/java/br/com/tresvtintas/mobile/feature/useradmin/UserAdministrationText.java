package br.com.tresvtintas.mobile.feature.useradmin;

import br.com.tresvtintas.mobile.core.model.AppRole;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationFailureKind;

final class UserAdministrationText {
    private UserAdministrationText() {
        throw new AssertionError("No instances.");
    }

    static int role(AppRole role) {
        return switch (role) {
            case MASTER_ADMIN -> R.string.user_admin_role_master;
            case MANAGER -> R.string.user_admin_role_manager;
            case SALESPERSON -> R.string.user_admin_role_salesperson;
            case DELIVERY_DRIVER -> R.string.user_admin_role_delivery;
            case PAINTER -> R.string.user_admin_role_painter;
            case CUSTOMER -> R.string.user_admin_role_customer;
            case USER -> R.string.user_admin_role_user;
        };
    }

    static int failure(UserAdministrationFailureKind kind) {
        return switch (kind) {
            case ACCESS_REVOKED, AUTH_REJECTED, FORBIDDEN ->
                    R.string.user_admin_error_access;
            case CONFLICT -> R.string.user_admin_error_conflict;
            case IDEMPOTENCY_IN_PROGRESS -> R.string.user_admin_error_in_progress;
            case IDEMPOTENCY_KEY_REUSED, INVALID_REQUEST ->
                    R.string.user_admin_error_invalid;
            case NETWORK -> R.string.user_admin_error_network;
            case NOT_FOUND -> R.string.user_admin_error_not_found;
            case RATE_LIMITED -> R.string.user_admin_error_rate_limited;
            case SERVICE_UNAVAILABLE -> R.string.user_admin_error_service;
            case UPDATE_REQUIRED -> R.string.user_admin_error_update;
            case PROTOCOL -> R.string.user_admin_error_protocol;
        };
    }
}
