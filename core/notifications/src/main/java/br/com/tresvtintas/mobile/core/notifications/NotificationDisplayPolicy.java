package br.com.tresvtintas.mobile.core.notifications;

import java.util.EnumSet;
import java.util.Set;

public record NotificationDisplayPolicy(
        boolean operationalEnabled,
        Set<NotificationCategory> enabledCategories,
        boolean essentialSecurityAlerts) {

    public NotificationDisplayPolicy {
        if (enabledCategories == null || !essentialSecurityAlerts) {
            throw new IllegalArgumentException(
                    "Notification display policy is invalid.");
        }
        Set<NotificationCategory> normalized = enabledCategories.isEmpty()
                ? Set.of()
                : EnumSet.copyOf(enabledCategories);
        if (normalized.stream().anyMatch(category -> !category.configurable())) {
            throw new IllegalArgumentException(
                    "Essential notification categories cannot be configured.");
        }
        enabledCategories = Set.copyOf(normalized);
    }

    public static NotificationDisplayPolicy from(
            NotificationPreferences preferences) {
        if (preferences == null) {
            throw new IllegalArgumentException(
                    "Notification preferences are required.");
        }
        Set<NotificationCategory> enabled = EnumSet.noneOf(
                NotificationCategory.class);
        for (NotificationCategory category : NotificationCategory.values()) {
            if (category.configurable() && preferences.enabled(category)) {
                enabled.add(category);
            }
        }
        return new NotificationDisplayPolicy(
                preferences.operationalEnabled(),
                enabled,
                preferences.essentialSecurityAlerts());
    }

    public boolean allows(NotificationMessage message) {
        if (message == null) {
            return false;
        }
        NotificationCategory category = message.category();
        return category.configurable()
                ? operationalEnabled && enabledCategories.contains(category)
                : essentialSecurityAlerts;
    }
}
