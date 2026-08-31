package br.com.tresvtintas.mobile.feature.painteradmin;

import android.app.Activity;
import android.view.WindowManager;

final class PainterAdministrationPrivacy {
    private PainterAdministrationPrivacy() {
        throw new AssertionError("No instances.");
    }

    static void protect(Activity activity) {
        activity.getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SECURE);
    }
}
