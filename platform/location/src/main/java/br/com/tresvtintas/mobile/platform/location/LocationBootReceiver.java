package br.com.tresvtintas.mobile.platform.location;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class LocationBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            return;
        }
        if (!LocationTrackingController.enabled(context)) {
            return;
        }
        LocationTrackingController.schedule(context);
        try {
            LocationTrackingController.start(context);
        } catch (IllegalStateException | SecurityException ignored) {
            // Periodic reconciliation retries after the authenticated runtime is restored.
        }
    }
}
