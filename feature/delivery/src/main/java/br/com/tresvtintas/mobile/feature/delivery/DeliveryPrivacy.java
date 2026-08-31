package br.com.tresvtintas.mobile.feature.delivery;

import android.app.Activity;
import android.view.WindowManager;

final class DeliveryPrivacy {
    private DeliveryPrivacy() {
        throw new AssertionError("No instances.");
    }

    static void protect(Activity activity) {
        activity.getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SECURE);
    }
}
