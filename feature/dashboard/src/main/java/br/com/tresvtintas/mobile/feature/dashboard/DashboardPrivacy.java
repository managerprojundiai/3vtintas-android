package br.com.tresvtintas.mobile.feature.dashboard;

import android.app.Activity;
import android.view.WindowManager;

final class DashboardPrivacy {
    private DashboardPrivacy() {
    }

    static void protect(Activity activity) {
        activity.getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SECURE);
    }
}
