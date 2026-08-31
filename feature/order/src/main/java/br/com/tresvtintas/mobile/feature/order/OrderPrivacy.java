package br.com.tresvtintas.mobile.feature.order;

import android.app.Activity;
import android.view.WindowManager;

final class OrderPrivacy {
    private OrderPrivacy() { }

    static void protect(Activity activity) {
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }
}
