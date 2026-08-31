package br.com.tresvtintas.mobile.core.notifications;

@FunctionalInterface
public interface NotificationSettingsStateListener {
    void onNotificationSettingsStateChanged(NotificationSettingsState state);
}
