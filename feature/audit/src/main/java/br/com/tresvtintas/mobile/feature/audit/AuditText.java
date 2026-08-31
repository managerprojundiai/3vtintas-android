package br.com.tresvtintas.mobile.feature.audit;

import br.com.tresvtintas.mobile.core.audit.AuditFailureKind;
import br.com.tresvtintas.mobile.core.model.AppRole;

final class AuditText {
    private AuditText() {
        throw new AssertionError("No instances.");
    }

    static int failure(AuditFailureKind kind) {
        return switch (kind) {
            case ACCESS_REVOKED, AUTH_REJECTED, FORBIDDEN ->
                    R.string.audit_error_access;
            case INVALID_REQUEST -> R.string.audit_error_invalid;
            case NETWORK -> R.string.audit_error_network;
            case RATE_LIMITED -> R.string.audit_error_rate_limited;
            case SERVICE_UNAVAILABLE -> R.string.audit_error_service;
            case UPDATE_REQUIRED -> R.string.audit_error_update;
            case PROTOCOL -> R.string.audit_error_protocol;
        };
    }

    static int role(AppRole role) {
        return switch (role) {
            case MASTER_ADMIN -> R.string.audit_role_master;
            case MANAGER -> R.string.audit_role_manager;
            case SALESPERSON -> R.string.audit_role_salesperson;
            case DELIVERY_DRIVER -> R.string.audit_role_delivery;
            case PAINTER -> R.string.audit_role_painter;
            case CUSTOMER -> R.string.audit_role_customer;
            case USER -> R.string.audit_role_user;
        };
    }
}
