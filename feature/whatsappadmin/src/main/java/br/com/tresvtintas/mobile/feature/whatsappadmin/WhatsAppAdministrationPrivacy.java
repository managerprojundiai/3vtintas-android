package br.com.tresvtintas.mobile.feature.whatsappadmin;

import android.app.Activity;
import android.view.WindowManager;

final class WhatsAppAdministrationPrivacy {
    private WhatsAppAdministrationPrivacy() {
        throw new AssertionError("No instances.");
    }

    static void protect(Activity activity) {
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }
}
