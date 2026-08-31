package br.com.tresvtintas.lab.autostart;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public final class ProbeForegroundService extends Service {
    private static final String CHANNEL_ID = "f703a_probe_active";
    private static final int NOTIFICATION_ID = 7303;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ensureNotificationChannel();
        try {
            startForeground(NOTIFICATION_ID, notification());
            ProbeStateStore.recordStarted(this);
        } catch (SecurityException | IllegalStateException failure) {
            ProbeStateStore.recordFailure(this, failure);
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        ProbeStateStore.recordStopped(this);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void ensureNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.probe_channel_name),
                NotificationManager.IMPORTANCE_LOW);
        NotificationManager manager =
                getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);
    }

    private Notification notification() {
        return new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.probe_notification_title))
                .setContentText(getString(R.string.probe_notification_text))
                .setSmallIcon(R.drawable.ic_probe)
                .setOngoing(true)
                .build();
    }
}
