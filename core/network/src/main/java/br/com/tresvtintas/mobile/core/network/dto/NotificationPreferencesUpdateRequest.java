package br.com.tresvtintas.mobile.core.network.dto;

import java.util.Set;

public record NotificationPreferencesUpdateRequest(
        String permissionState,
        boolean operationalEnabled,
        NotificationCategoriesDto categories,
        int expectedRevision) {
    private static final Set<String> PERMISSION_STATES =
            Set.of("unknown", "granted", "denied");

    public NotificationPreferencesUpdateRequest {
        if (!PERMISSION_STATES.contains(permissionState)
                || categories == null
                || expectedRevision < 0) {
            throw new IllegalArgumentException(
                    "Notification preferences update is invalid.");
        }
    }
}
