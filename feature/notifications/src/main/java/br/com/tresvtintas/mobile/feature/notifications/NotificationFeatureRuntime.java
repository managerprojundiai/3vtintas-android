package br.com.tresvtintas.mobile.feature.notifications;

import br.com.tresvtintas.mobile.core.notifications.NotificationPermissionController;
import br.com.tresvtintas.mobile.core.notifications.NotificationSettingsController;
import java.util.Objects;

public record NotificationFeatureRuntime(
        NotificationSettingsController controller,
        NotificationPermissionController permissionController,
        boolean pushAvailable) {

    public NotificationFeatureRuntime {
        Objects.requireNonNull(
                controller,
                "Notification controller is required.");
        Objects.requireNonNull(
                permissionController,
                "Notification permission gateway is required.");
    }
}
