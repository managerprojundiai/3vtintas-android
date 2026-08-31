package br.com.tresvtintas.mobile.feature.attendance;

import android.app.Activity;
import android.view.WindowManager;

final class AttendancePrivacy {
    private AttendancePrivacy() {
        throw new AssertionError("No instances.");
    }

    static void protect(Activity activity) {
        activity.getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SECURE);
    }
}
