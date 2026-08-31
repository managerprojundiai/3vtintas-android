package br.com.tresvtintas.mobile.platform.notifications;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import br.com.tresvtintas.mobile.core.notifications.NotificationPermissionController;
import br.com.tresvtintas.mobile.core.notifications.NotificationPermissionState;

public final class AndroidNotificationPermissionController
        implements NotificationPermissionController {
    private static final String PREFERENCES_NAME =
            "three_v_notification_permission";
    private static final String REQUEST_RECORDED = "request_recorded";
    private final Context context;

    public AndroidNotificationPermissionController(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context is required.");
        }
        this.context = context.getApplicationContext();
    }

    @Override
    public NotificationPermissionState currentState() {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return requestRecorded()
                    ? NotificationPermissionState.DENIED
                    : NotificationPermissionState.UNKNOWN;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return NotificationPermissionState.GRANTED;
        }
        int permission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS);
        if (permission == PackageManager.PERMISSION_GRANTED) {
            return NotificationPermissionState.GRANTED;
        }
        return requestRecorded()
                ? NotificationPermissionState.DENIED
                : NotificationPermissionState.UNKNOWN;
    }

    @Override
    public boolean requiresRuntimeRequest() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void recordDecision(boolean granted) {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(REQUEST_RECORDED, true)
                .apply();
        if (granted && currentState() != NotificationPermissionState.GRANTED) {
            throw new IllegalStateException(
                    "Android did not grant notification permission.");
        }
    }

    private boolean requestRecorded() {
        return context.getSharedPreferences(
                        PREFERENCES_NAME,
                        Context.MODE_PRIVATE)
                .getBoolean(REQUEST_RECORDED, false);
    }
}
