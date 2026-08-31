package br.com.tresvtintas.mobile.platform.notifications;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import br.com.tresvtintas.mobile.core.notifications.NotificationCategory;
import br.com.tresvtintas.mobile.core.notifications.NotificationMessage;
import br.com.tresvtintas.mobile.core.notifications.NotificationRoute;

public final class NotificationPublisher {
    private final Context context;

    public NotificationPublisher(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context is required.");
        }
        this.context = context.getApplicationContext();
    }

    public void publish(NotificationMessage message) {
        if (message == null
                || Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED
                || !NotificationManagerCompat.from(context)
                        .areNotificationsEnabled()) {
            return;
        }
        Intent launch = context.getPackageManager()
                .getLaunchIntentForPackage(context.getPackageName());
        if (launch == null) {
            return;
        }
        launch.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        launch.putExtra(
                NotificationRoute.INTENT_EXTRA,
                message.route().wireValue());
        launch.putExtra(
                NotificationRoute.EVENT_ID_EXTRA,
                message.eventId().toString());
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                message.eventId().hashCode(),
                launch,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context,
                NotificationChannels.forCategory(message.category()))
                .setSmallIcon(R.drawable.notification_ic_status)
                .setContentTitle(context.getString(title(message.category())))
                .setContentText(context.getString(R.string.notification_generic_body))
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent);
        NotificationManagerCompat.from(context).notify(
                message.eventId().hashCode(),
                builder.build());
    }

    private static int title(NotificationCategory category) {
        return switch (category) {
            case ATTENDANCE -> R.string.notification_title_attendance;
            case ORDERS -> R.string.notification_title_orders;
            case DELIVERIES -> R.string.notification_title_deliveries;
            case QUOTES -> R.string.notification_title_quotes;
            case COMMISSIONS -> R.string.notification_title_commissions;
            case APPOINTMENTS -> R.string.notification_title_appointments;
            case FINANCE -> R.string.notification_title_finance;
            case AGENT -> R.string.notification_title_agent;
            case SECURITY -> R.string.notification_title_security;
        };
    }
}
