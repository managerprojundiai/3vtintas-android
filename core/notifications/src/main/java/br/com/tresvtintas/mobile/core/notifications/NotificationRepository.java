package br.com.tresvtintas.mobile.core.notifications;

import java.util.Map;

public interface NotificationRepository {
    NotificationPreferences load() throws NotificationException;

    NotificationPreferences update(
            NotificationPermissionState permissionState,
            boolean operationalEnabled,
            Map<NotificationCategory, Boolean> categories,
            int expectedRevision) throws NotificationException;

    void register(String firebaseInstallationId) throws NotificationException;

    void unregister() throws NotificationException;
}
