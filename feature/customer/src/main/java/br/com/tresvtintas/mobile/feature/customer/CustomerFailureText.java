package br.com.tresvtintas.mobile.feature.customer;

import androidx.annotation.StringRes;
import br.com.tresvtintas.mobile.core.customer.CustomerFailureKind;

final class CustomerFailureText {
    private CustomerFailureText() {
        throw new AssertionError("No instances.");
    }

    @StringRes
    static int resource(CustomerFailureKind kind) {
        return switch (kind) {
            case ACCESS_REVOKED -> R.string.customer_error_access;
            case AUTH_REJECTED -> R.string.customer_error_auth;
            case CONFLICT -> R.string.customer_error_conflict;
            case FORBIDDEN -> R.string.customer_error_forbidden;
            case IDEMPOTENCY_IN_PROGRESS ->
                    R.string.customer_error_in_progress;
            case IDEMPOTENCY_KEY_REUSED ->
                    R.string.customer_error_key_reused;
            case INVALID_REQUEST -> R.string.customer_error_invalid;
            case NETWORK -> R.string.customer_error_network;
            case NOT_FOUND -> R.string.customer_error_not_found;
            case PROTOCOL -> R.string.customer_error_protocol;
            case RATE_LIMITED -> R.string.customer_error_rate;
            case SERVICE_UNAVAILABLE -> R.string.customer_error_service;
            case UPDATE_REQUIRED -> R.string.customer_error_update;
        };
    }
}
