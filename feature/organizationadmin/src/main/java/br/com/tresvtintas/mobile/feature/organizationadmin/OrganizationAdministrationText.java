package br.com.tresvtintas.mobile.feature.organizationadmin;

import android.content.Context;
import br.com.tresvtintas.mobile.core.organizationadmin.OrganizationAdministrationFailureKind;
import br.com.tresvtintas.mobile.core.organizationadmin.OrganizationAdministrationStatus;

final class OrganizationAdministrationText {
    private OrganizationAdministrationText() {
        throw new AssertionError("No instances.");
    }

    static String status(
            Context context,
            OrganizationAdministrationStatus status) {
        int resource = switch (status) {
            case ACTIVE -> R.string.organization_admin_status_active;
            case BLOCKED -> R.string.organization_admin_status_blocked;
            case PENDING -> R.string.organization_admin_status_pending;
        };
        return context.getString(resource);
    }

    static String failure(
            Context context,
            OrganizationAdministrationFailureKind kind) {
        int resource = switch (kind) {
            case AUTH_REJECTED, ACCESS_REVOKED ->
                    R.string.organization_admin_error_session;
            case FORBIDDEN -> R.string.organization_admin_error_forbidden;
            case NOT_FOUND -> R.string.organization_admin_error_not_found;
            case CONFLICT, LAST_ACTIVE, SLUG_CONFLICT ->
                    R.string.organization_admin_error_conflict;
            case INVALID_REQUEST -> R.string.organization_admin_error_invalid;
            case NETWORK, RATE_LIMITED, SERVICE_UNAVAILABLE,
                    IDEMPOTENCY_IN_PROGRESS ->
                    R.string.organization_admin_error_network;
            case UPDATE_REQUIRED -> R.string.organization_admin_error_update;
            case IDEMPOTENCY_KEY_REUSED, PROTOCOL ->
                    R.string.organization_admin_error_protocol;
        };
        return context.getString(resource);
    }
}
