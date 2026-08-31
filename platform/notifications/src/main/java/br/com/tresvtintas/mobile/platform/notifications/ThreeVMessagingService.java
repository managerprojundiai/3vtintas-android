package br.com.tresvtintas.mobile.platform.notifications;

import br.com.tresvtintas.mobile.core.notifications.NotificationMessage;
import br.com.tresvtintas.mobile.core.notifications.NotificationPushRegistry;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public final class ThreeVMessagingService extends FirebaseMessagingService {
    @Override
    public void onRegistered(String installationId) {
        NotificationPushRegistry.registered(installationId);
    }

    @Override
    public void onUnregistered(String installationId) {
        NotificationPushRegistry.unregistered();
    }

    @Override
    public void onNewToken(String token) {
        FirebaseMessaging.getInstance().register();
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        NotificationMessage.parse(message.getData())
                .ifPresent(NotificationPushRegistry::message);
    }

    @Override
    public void onDeletedMessages() {
        NotificationPushRegistry.messagesDeleted();
    }
}
