package br.com.tresvtintas.mobile.core.notifications;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public record NotificationPreferences(
        NotificationPermissionState permissionState,
        boolean operationalEnabled,
        Map<NotificationCategory, Boolean> categories,
        boolean essentialSecurityAlerts,
        int revision,
        Optional<Instant> updatedAt) {

    public NotificationPreferences {
        if (permissionState == null || revision < 0 || !essentialSecurityAlerts) {
            throw new IllegalArgumentException("Notification preferences are invalid.");
        }
        Map<NotificationCategory, Boolean> normalized =
                new EnumMap<>(NotificationCategory.class);
        if (categories != null) {
            normalized.putAll(categories);
        }
        for (NotificationCategory category : NotificationCategory.values()) {
            if (category.configurable() && !normalized.containsKey(category)) {
                throw new IllegalArgumentException(
                        "A notification category preference is missing.");
            }
        }
        normalized.remove(NotificationCategory.SECURITY);
        categories = Map.copyOf(normalized);
        updatedAt = updatedAt == null ? Optional.empty() : updatedAt;
    }

    public boolean enabled(NotificationCategory category) {
        if (category == null) {
            return false;
        }
        return category == NotificationCategory.SECURITY
                ? essentialSecurityAlerts
                : categories.getOrDefault(category, false);
    }

    public NotificationPreferences withPermission(
            NotificationPermissionState permission) {
        return new NotificationPreferences(
                permission,
                operationalEnabled,
                categories,
                essentialSecurityAlerts,
                revision,
                updatedAt);
    }

    public NotificationPreferences withSelection(
            boolean enabled,
            Map<NotificationCategory, Boolean> selected) {
        return new NotificationPreferences(
                permissionState,
                enabled,
                selected,
                essentialSecurityAlerts,
                revision,
                updatedAt);
    }
}
