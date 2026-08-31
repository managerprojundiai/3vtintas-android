package br.com.tresvtintas.mobile.feature.quote;

import android.app.Activity;
import android.view.WindowManager;

final class MaterialQuotePrivacy {
    private MaterialQuotePrivacy() {
    }

    static void protect(Activity activity) {
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }
}
