package br.com.tresvtintas.mobile.platform.location;

import android.content.Context;
import android.content.Intent;
import androidx.core.content.ContextCompat;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.time.Duration;

public final class LocationTrackingController {
    static final String PREFERENCES = "3v_workforce_location_state";
    static final String ENABLED = "enabled";
    static final String LAST_SERVICE_STATE = "last_service_state";
    private static final String RECONCILE_WORK = "3v-workforce-location-reconcile";

    private LocationTrackingController() {
        throw new AssertionError("No instances.");
    }

    public static void enable(Context context) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(ENABLED, true)
                .apply();
        schedule(context);
        start(context);
    }

    public static void disable(Context context) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(ENABLED, false)
                .putString(LAST_SERVICE_STATE, "stopped")
                .apply();
        context.stopService(new Intent(context, WorkforceLocationService.class));
        WorkManager.getInstance(context).cancelUniqueWork(RECONCILE_WORK);
    }

    public static boolean enabled(Context context) {
        return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .getBoolean(ENABLED, false);
    }

    public static String lastServiceState(Context context) {
        return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .getString(LAST_SERVICE_STATE, "stopped");
    }

    public static void start(Context context) {
        if (!LocationPermissions.canStartTrackingService(context)) {
            publishState(context, "permission_required");
            return;
        }
        ContextCompat.startForegroundService(
                context,
                new Intent(context, WorkforceLocationService.class));
    }

    public static void schedule(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                LocationReconcileWorker.class,
                Duration.ofMinutes(15))
                .setConstraints(constraints)
                .build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                RECONCILE_WORK,
                ExistingPeriodicWorkPolicy.UPDATE,
                request);
    }

    static void publishState(Context context, String state) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .edit()
                .putString(LAST_SERVICE_STATE, state)
                .apply();
    }
}
