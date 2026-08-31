package br.com.tresvtintas.mobile.platform.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import br.com.tresvtintas.mobile.core.notifications.NotificationCategory;
import java.util.List;

public final class NotificationChannels {
    static final String SECURITY = "three_v_security";
    static final String ATTENDANCE = "three_v_attendance";
    static final String OPERATIONS = "three_v_operations";
    static final String PLANNING = "three_v_planning";
    static final String ASSISTANT = "three_v_assistant";

    private NotificationChannels() {
        throw new AssertionError("No instances.");
    }

    public static void ensureCreated(Context context) {
        NotificationManager manager = context.getSystemService(
                NotificationManager.class);
        if (manager == null) {
            return;
        }
        manager.createNotificationChannels(List.of(
                channel(
                        context,
                        SECURITY,
                        R.string.notification_channel_security,
                        R.string.notification_channel_security_description,
                        NotificationManager.IMPORTANCE_HIGH),
                channel(
                        context,
                        ATTENDANCE,
                        R.string.notification_channel_attendance,
                        R.string.notification_channel_attendance_description,
                        NotificationManager.IMPORTANCE_DEFAULT),
                channel(
                        context,
                        OPERATIONS,
                        R.string.notification_channel_operations,
                        R.string.notification_channel_operations_description,
                        NotificationManager.IMPORTANCE_DEFAULT),
                channel(
                        context,
                        PLANNING,
                        R.string.notification_channel_planning,
                        R.string.notification_channel_planning_description,
                        NotificationManager.IMPORTANCE_DEFAULT),
                channel(
                        context,
                        ASSISTANT,
                        R.string.notification_channel_assistant,
                        R.string.notification_channel_assistant_description,
                        NotificationManager.IMPORTANCE_LOW)));
    }

    static String forCategory(NotificationCategory category) {
        return switch (category) {
            case SECURITY -> SECURITY;
            case ATTENDANCE -> ATTENDANCE;
            case ORDERS, DELIVERIES, QUOTES -> OPERATIONS;
            case COMMISSIONS, APPOINTMENTS, FINANCE -> PLANNING;
            case AGENT -> ASSISTANT;
        };
    }

    private static NotificationChannel channel(
            Context context,
            String id,
            int name,
            int description,
            int importance) {
        NotificationChannel channel = new NotificationChannel(
                id,
                context.getString(name),
                importance);
        channel.setDescription(context.getString(description));
        channel.setShowBadge(true);
        return channel;
    }
}
