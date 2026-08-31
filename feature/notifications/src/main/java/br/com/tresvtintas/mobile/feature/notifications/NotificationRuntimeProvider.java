package br.com.tresvtintas.mobile.feature.notifications;

import java.util.Optional;

@FunctionalInterface
public interface NotificationRuntimeProvider {
    Optional<NotificationFeatureRuntime> notificationRuntime();
}
