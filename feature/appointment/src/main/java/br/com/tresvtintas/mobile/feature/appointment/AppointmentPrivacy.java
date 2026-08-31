package br.com.tresvtintas.mobile.feature.appointment;

import android.app.Activity;
import android.view.WindowManager;

final class AppointmentPrivacy {
    private AppointmentPrivacy() {
    }

    static void protect(Activity activity) {
        activity.getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SECURE);
    }
}
