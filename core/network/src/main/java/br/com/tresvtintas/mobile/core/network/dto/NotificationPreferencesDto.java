package br.com.tresvtintas.mobile.core.network.dto;

import java.util.Set;

public record NotificationPreferencesDto(
        String permissionState,
        boolean operationalEnabled,
        NotificationCategoriesDto categories,
        boolean essentialSecurityAlerts,
        int revision,
        String updatedAt) {
    private static final Set<String> PERMISSION_STATES =
            Set.of("unknown", "granted", "denied");

    public NotificationPreferencesDto {
        if (!PERMISSION_STATES.contains(permissionState)
                || categories == null
                || !essentialSecurityAlerts
                || revision < 0) {
            throw new IllegalArgumentException(
                    "Notification preferences response is invalid.");
        }
        if (updatedAt != null) {
            updatedAt = DtoValidation.requireInstant(
                    updatedAt,
                    "Notification preferences update time");
        }
    }
}
