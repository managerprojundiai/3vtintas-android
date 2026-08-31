package br.com.tresvtintas.mobile.core.notifications;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class NotificationPushRegistry {
    private static final AtomicReference<NotificationPushSink> CURRENT =
            new AtomicReference<>();

    private NotificationPushRegistry() {
        throw new AssertionError("No instances.");
    }

    public static void install(NotificationPushSink sink) {
        CURRENT.set(Objects.requireNonNull(sink, "Notification push sink is required."));
    }

    public static void remove(NotificationPushSink sink) {
        CURRENT.compareAndSet(sink, null);
    }

    public static void registered(String firebaseInstallationId) {
        NotificationPushSink sink = CURRENT.get();
        if (sink != null) {
            sink.onRegistered(firebaseInstallationId);
        }
    }

    public static void unregistered() {
        NotificationPushSink sink = CURRENT.get();
        if (sink != null) {
            sink.onUnregistered();
        }
    }

    public static void message(NotificationMessage message) {
        NotificationPushSink sink = CURRENT.get();
        if (sink != null) {
            sink.onMessage(message);
        }
    }

    public static void messagesDeleted() {
        NotificationPushSink sink = CURRENT.get();
        if (sink != null) {
            sink.onMessagesDeleted();
        }
    }

    public static void transportFailure() {
        NotificationPushSink sink = CURRENT.get();
        if (sink != null) {
            sink.onTransportFailure();
        }
    }
}
