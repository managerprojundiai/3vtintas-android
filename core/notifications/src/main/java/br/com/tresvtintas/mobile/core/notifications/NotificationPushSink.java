package br.com.tresvtintas.mobile.core.notifications;

public interface NotificationPushSink {
    void onRegistered(String firebaseInstallationId);

    void onUnregistered();

    void onMessage(NotificationMessage message);

    void onMessagesDeleted();

    void onTransportFailure();
}
