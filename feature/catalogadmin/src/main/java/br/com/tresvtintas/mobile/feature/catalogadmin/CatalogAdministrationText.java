package br.com.tresvtintas.mobile.feature.catalogadmin;

import android.content.Context;
import androidx.annotation.StringRes;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationFailureKind;

final class CatalogAdministrationText {
    private CatalogAdministrationText() {
        throw new AssertionError("No instances.");
    }

    static String failure(
            Context context,
            CatalogAdministrationFailureKind kind) {
        return context.getString(failureResource(kind));
    }

    @StringRes
    private static int failureResource(
            CatalogAdministrationFailureKind kind) {
        return switch (kind) {
            case AUTH_REJECTED, ACCESS_REVOKED ->
                    R.string.catalog_admin_error_session;
            case FORBIDDEN -> R.string.catalog_admin_error_forbidden;
            case NOT_FOUND -> R.string.catalog_admin_error_not_found;
            case CONFLICT, IDEMPOTENCY_IN_PROGRESS,
                    IDEMPOTENCY_KEY_REUSED ->
                    R.string.catalog_admin_error_conflict;
            case INVALID_REQUEST -> R.string.catalog_admin_error_invalid;
            case PAYLOAD_TOO_LARGE ->
                    R.string.catalog_import_file_too_large;
            case UPDATE_REQUIRED -> R.string.catalog_admin_error_update;
            case NETWORK, RATE_LIMITED, SERVICE_UNAVAILABLE ->
                    R.string.catalog_admin_error_network;
            case PROTOCOL -> R.string.catalog_admin_error_protocol;
        };
    }
}
