package br.com.tresvtintas.mobile.feature.commission;

import android.app.Activity;
import android.view.WindowManager;

final class CommissionPrivacy {
    private CommissionPrivacy() {
    }

    static void protect(Activity activity) {
        activity.getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SECURE);
    }
}
