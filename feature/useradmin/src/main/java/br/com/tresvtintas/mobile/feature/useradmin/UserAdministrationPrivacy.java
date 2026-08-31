package br.com.tresvtintas.mobile.feature.useradmin;

import android.app.Activity;
import android.view.WindowManager;

final class UserAdministrationPrivacy {
    private UserAdministrationPrivacy() {
        throw new AssertionError("No instances.");
    }

    static void protect(Activity activity) {
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }
}
