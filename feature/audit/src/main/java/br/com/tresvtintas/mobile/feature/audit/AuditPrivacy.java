package br.com.tresvtintas.mobile.feature.audit;

import android.app.Activity;
import android.view.WindowManager;

final class AuditPrivacy {
    private AuditPrivacy() {
        throw new AssertionError("No instances.");
    }

    static void protect(Activity activity) {
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }
}
