package br.com.tresvtintas.mobile.platform.location;

import android.content.Context;
import android.os.BatteryManager;
import androidx.core.content.ContextCompat;

final class BatteryState {
    private BatteryState() {
        throw new AssertionError("No instances.");
    }

    static Integer percent(Context context) {
        BatteryManager manager = ContextCompat.getSystemService(
                context,
                BatteryManager.class);
        if (manager == null) {
            return null;
        }
        int value = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        return value >= 0 && value <= 100 ? value : null;
    }
}
