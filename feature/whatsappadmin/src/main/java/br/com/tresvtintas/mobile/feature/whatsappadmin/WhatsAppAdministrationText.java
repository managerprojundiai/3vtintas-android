package br.com.tresvtintas.mobile.feature.whatsappadmin;

import android.content.Context;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppAdministrationFailureKind;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppChannelMode;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppConnectionStatus;
import br.com.tresvtintas.mobile.core.whatsappadmin.WhatsAppStoreStatus;

final class WhatsAppAdministrationText {
    private WhatsAppAdministrationText() {
        throw new AssertionError("No instances.");
    }

    static String mode(Context context, WhatsAppChannelMode mode) {
        return context.getString(switch (mode) {
            case DISABLED -> R.string.whatsapp_admin_mode_disabled;
            case META -> R.string.whatsapp_admin_mode_meta;
            case EVOLUTION -> R.string.whatsapp_admin_mode_evolution;
            case BOTH -> R.string.whatsapp_admin_mode_both;
        });
    }

    static String connectionStatus(
            Context context,
            WhatsAppConnectionStatus status) {
        return context.getString(switch (status) {
            case PENDING -> R.string.whatsapp_admin_status_pending;
            case CONNECTING -> R.string.whatsapp_admin_status_connecting;
            case CONNECTED -> R.string.whatsapp_admin_status_connected;
            case DISCONNECTED -> R.string.whatsapp_admin_status_disconnected;
            case ERROR -> R.string.whatsapp_admin_status_error;
            case DISABLED -> R.string.whatsapp_admin_status_disabled;
        });
    }

    static String storeStatus(Context context, WhatsAppStoreStatus status) {
        return context.getString(switch (status) {
            case PENDING -> R.string.whatsapp_admin_store_pending;
            case ACTIVE -> R.string.whatsapp_admin_store_active;
            case BLOCKED -> R.string.whatsapp_admin_store_blocked;
        });
    }

    static String failure(
            Context context,
            WhatsAppAdministrationFailureKind kind) {
        return context.getString(switch (kind) {
            case AUTH_REJECTED -> R.string.whatsapp_admin_failure_auth;
            case FORBIDDEN -> R.string.whatsapp_admin_failure_forbidden;
            case INVALID_REQUEST -> R.string.whatsapp_admin_failure_invalid;
            case CONFLICT -> R.string.whatsapp_admin_failure_conflict;
            case IDEMPOTENCY_IN_PROGRESS ->
                    R.string.whatsapp_admin_failure_in_progress;
            case IDEMPOTENCY_KEY_REUSED ->
                    R.string.whatsapp_admin_failure_reused;
            case RATE_LIMITED -> R.string.whatsapp_admin_failure_rate;
            case SERVICE_UNAVAILABLE -> R.string.whatsapp_admin_failure_service;
            case UPDATE_REQUIRED -> R.string.whatsapp_admin_failure_update;
            case NETWORK -> R.string.whatsapp_admin_failure_network;
            case PROTOCOL -> R.string.whatsapp_admin_failure_protocol;
            case ACCESS_REVOKED -> R.string.whatsapp_admin_failure_revoked;
        });
    }
}
