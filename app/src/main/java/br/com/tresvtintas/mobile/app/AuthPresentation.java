package br.com.tresvtintas.mobile.app;

import br.com.tresvtintas.mobile.core.auth.AuthFailureKind;
import br.com.tresvtintas.mobile.core.auth.AuthState;
import java.util.Optional;

record AuthPresentation(
        int title,
        int message,
        Action primaryAction,
        int primaryLabel,
        boolean busy,
        boolean showLogout) {

    enum Action {
        NONE,
        SIGN_IN,
        RETRY_RESTORE
    }

    static AuthPresentation configurationRequired() {
        return new AuthPresentation(
                R.string.auth_configuration_title,
                R.string.auth_configuration_message,
                Action.NONE,
                0,
                false,
                false);
    }

    static AuthPresentation startupFailure(AuthFailureKind failure) {
        int message = failure == AuthFailureKind.CONFIGURATION
                ? R.string.auth_startup_configuration_message
                : failureMessage(failure);
        return new AuthPresentation(
                R.string.auth_error_title,
                message,
                Action.NONE,
                0,
                false,
                false);
    }

    static AuthPresentation from(AuthState state) {
        return switch (state.phase()) {
            case RESTORING -> busy(
                    R.string.auth_restoring_title,
                    R.string.auth_restoring_message);
            case SIGNING_IN -> busy(
                    R.string.auth_signing_in_title,
                    R.string.auth_signing_in_message);
            case SIGNING_OUT -> busy(
                    R.string.auth_signing_out_title,
                    R.string.auth_signing_out_message);
            case AUTHENTICATED -> new AuthPresentation(
                    R.string.auth_ready_title,
                    R.string.auth_ready_message,
                    Action.NONE,
                    0,
                    false,
                    true);
            case SIGNED_OUT -> signedOut(state.failure());
            case ERROR -> error(state.failure().orElse(AuthFailureKind.PROTOCOL));
        };
    }

    private static AuthPresentation signedOut(Optional<AuthFailureKind> warning) {
        int message = warning.map(AuthPresentation::signedOutMessage)
                .orElse(R.string.auth_signed_out_message);
        return new AuthPresentation(
                R.string.auth_signed_out_title,
                message,
                Action.SIGN_IN,
                R.string.auth_google_button,
                false,
                false);
    }

    private static AuthPresentation error(AuthFailureKind failure) {
        Action action = failure == AuthFailureKind.UPDATE_REQUIRED
                || failure == AuthFailureKind.CONFIGURATION
                ? Action.NONE
                : Action.RETRY_RESTORE;
        return new AuthPresentation(
                R.string.auth_error_title,
                failureMessage(failure),
                action,
                action == Action.NONE ? 0 : R.string.auth_retry_button,
                false,
                false);
    }

    private static AuthPresentation busy(int title, int message) {
        return new AuthPresentation(title, message, Action.NONE, 0, true, false);
    }

    private static int signedOutMessage(AuthFailureKind warning) {
        return switch (warning) {
            case CANCELED -> R.string.auth_canceled_message;
            case NO_CREDENTIAL -> R.string.auth_no_credential_message;
            default -> R.string.auth_local_logout_warning;
        };
    }

    private static int failureMessage(AuthFailureKind failure) {
        return switch (failure) {
            case AUTH_REJECTED -> R.string.auth_rejected_message;
            case CONFIGURATION -> R.string.auth_configuration_message;
            case NETWORK -> R.string.auth_network_message;
            case RATE_LIMITED -> R.string.auth_rate_limited_message;
            case SERVICE_UNAVAILABLE -> R.string.auth_service_message;
            case STORAGE -> R.string.auth_storage_message;
            case UPDATE_REQUIRED -> R.string.auth_update_message;
            default -> R.string.auth_protocol_message;
        };
    }
}
