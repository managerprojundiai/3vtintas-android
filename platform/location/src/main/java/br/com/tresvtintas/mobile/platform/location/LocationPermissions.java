package br.com.tresvtintas.mobile.platform.location;

import android.Manifest;
import android.content.Context;
import android.location.LocationManager;
import android.os.Build;
import androidx.core.content.ContextCompat;
import br.com.tresvtintas.mobile.core.location.WorkforceLocationHost;
import br.com.tresvtintas.mobile.core.network.dto.LocationDtos;

public final class LocationPermissions {
    private LocationPermissions() {
        throw new AssertionError("No instances.");
    }

    public static boolean hasForeground(Context context) {
        return granted(context, Manifest.permission.ACCESS_FINE_LOCATION)
                || granted(context, Manifest.permission.ACCESS_COARSE_LOCATION);
    }

    public static boolean hasPrecise(Context context) {
        return granted(context, Manifest.permission.ACCESS_FINE_LOCATION);
    }

    public static boolean hasBackground(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                || granted(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION);
    }

    public static boolean hasNotifications(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || granted(context, Manifest.permission.POST_NOTIFICATIONS);
    }

    public static boolean canStartTrackingService(Context context) {
        return hasPrecise(context)
                && hasBackground(context)
                && hasNotifications(context);
    }

    public static boolean locationEnabled(Context context) {
        LocationManager manager = ContextCompat.getSystemService(
                context,
                LocationManager.class);
        if (manager == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return manager.isLocationEnabled();
        }
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    public static LocationDtos.DeviceReport report(
            Context context,
            WorkforceLocationHost host,
            String serviceState) {
        return new LocationDtos.DeviceReport(
                hasPrecise(context)
                        ? "precise"
                        : hasForeground(context) ? "approximate" : "denied",
                hasBackground(context) ? "granted" : "denied",
                hasNotifications(context) ? "granted" : "denied",
                locationEnabled(context),
                serviceState,
                BatteryState.percent(context),
                host.workforceLocationAppVersion());
    }

    private static boolean granted(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }
}
