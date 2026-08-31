package br.com.tresvtintas.mobile.feature.laborquote;

import android.app.Activity;
import android.view.WindowManager;

final class LaborQuotePrivacy {
    private LaborQuotePrivacy() {
    }

    static void protect(Activity activity) {
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }
}
