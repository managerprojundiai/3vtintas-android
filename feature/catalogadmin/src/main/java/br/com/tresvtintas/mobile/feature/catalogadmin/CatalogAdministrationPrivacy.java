package br.com.tresvtintas.mobile.feature.catalogadmin;

import android.app.Activity;
import android.app.Dialog;
import android.view.Window;
import android.view.WindowManager;

final class CatalogAdministrationPrivacy {
    private CatalogAdministrationPrivacy() {
        throw new AssertionError("No instances.");
    }

    static void protect(Activity activity) {
        activity.getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SECURE);
    }

    static void protect(Dialog dialog) {
        Window window = dialog.getWindow();
        if (window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }
}
