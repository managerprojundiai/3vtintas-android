package br.com.tresvtintas.mobile.platform.notifications;

import android.content.Context;
import br.com.tresvtintas.mobile.core.notifications.FirebaseClientConfiguration;
import br.com.tresvtintas.mobile.core.notifications.NotificationPushRegistry;
import br.com.tresvtintas.mobile.core.notifications.NotificationPushTransport;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;

public final class FirebasePushTransport implements NotificationPushTransport {
    private final boolean available;

    public FirebasePushTransport(
            Context context,
            FirebaseClientConfiguration configuration) {
        if (context == null || configuration == null) {
            throw new IllegalArgumentException(
                    "Firebase transport dependencies are required.");
        }
        available = initialize(context.getApplicationContext(), configuration);
    }

    @Override
    public boolean available() {
        return available;
    }

    @Override
    public void register() {
        if (!available) {
            NotificationPushRegistry.transportFailure();
            return;
        }
        try {
            FirebaseMessaging.getInstance()
                    .register()
                    .addOnFailureListener(ignored ->
                            NotificationPushRegistry.transportFailure());
        } catch (IllegalStateException exception) {
            NotificationPushRegistry.transportFailure();
        }
    }

    @Override
    public void unregister() {
        if (!available) {
            return;
        }
        try {
            FirebaseMessaging.getInstance()
                    .unregister()
                    .addOnFailureListener(ignored ->
                            NotificationPushRegistry.transportFailure());
        } catch (IllegalStateException exception) {
            NotificationPushRegistry.transportFailure();
        }
    }

    private static boolean initialize(
            Context context,
            FirebaseClientConfiguration configuration) {
        if (!configuration.configured()) {
            return false;
        }
        try {
            FirebaseApp app;
            try {
                app = FirebaseApp.getInstance();
            } catch (IllegalStateException missingDefaultApp) {
                FirebaseOptions options = new FirebaseOptions.Builder()
                        .setApplicationId(configuration.applicationId())
                        .setProjectId(configuration.projectId())
                        .setApiKey(configuration.apiKey())
                        .setGcmSenderId(configuration.senderId())
                        .build();
                app = FirebaseApp.initializeApp(context, options);
            }
            if (app == null) {
                return false;
            }
            FirebaseMessaging.getInstance().setAutoInitEnabled(false);
            return true;
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return false;
        }
    }
}
