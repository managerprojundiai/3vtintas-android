package br.com.tresvtintas.mobile.feature.finance;

import android.app.Activity;
import android.view.WindowManager;

public final class FinancePrivacy {
    private FinancePrivacy() {
    }

    public static void protect(Activity activity) {
        activity.getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SECURE);
    }
}
