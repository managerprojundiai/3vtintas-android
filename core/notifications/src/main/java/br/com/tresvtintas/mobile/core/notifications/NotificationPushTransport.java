package br.com.tresvtintas.mobile.core.notifications;

public interface NotificationPushTransport {
    boolean available();

    void register();

    void unregister();
}
