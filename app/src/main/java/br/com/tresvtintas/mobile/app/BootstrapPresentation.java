package br.com.tresvtintas.mobile.app;

import br.com.tresvtintas.mobile.core.bootstrap.BootstrapFailureKind;
import br.com.tresvtintas.mobile.core.bootstrap.BootstrapState;

record BootstrapPresentation(
        int title,
        int message,
        boolean busy,
        boolean retryAllowed) {

    static BootstrapPresentation from(BootstrapState state) {
        if (state.phase() == BootstrapState.Phase.LOADING) {
            return new BootstrapPresentation(
                    R.string.bootstrap_loading_title,
                    R.string.bootstrap_loading_message,
                    true,
                    false);
        }
        BootstrapFailureKind failure = state.failure()
                .orElse(BootstrapFailureKind.PROTOCOL);
        return new BootstrapPresentation(
                R.string.bootstrap_error_title,
                failureMessage(failure),
                false,
                retryAllowed(failure));
    }

    private static int failureMessage(BootstrapFailureKind failure) {
        return switch (failure) {
            case DEVICE_UNSUPPORTED -> R.string.bootstrap_device_unsupported_message;
            case MAINTENANCE -> R.string.bootstrap_maintenance_message;
            case NETWORK -> R.string.bootstrap_network_message;
            case RATE_LIMITED -> R.string.bootstrap_rate_limited_message;
            case SERVICE_UNAVAILABLE -> R.string.bootstrap_service_message;
            case UPDATE_REQUIRED -> R.string.auth_update_message;
            default -> R.string.bootstrap_protocol_message;
        };
    }

    private static boolean retryAllowed(BootstrapFailureKind failure) {
        return switch (failure) {
            case NETWORK, RATE_LIMITED, SERVICE_UNAVAILABLE, MAINTENANCE -> true;
            default -> false;
        };
    }
}
