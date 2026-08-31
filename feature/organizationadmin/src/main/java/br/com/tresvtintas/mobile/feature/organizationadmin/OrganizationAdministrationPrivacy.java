package br.com.tresvtintas.mobile.feature.organizationadmin;

import android.app.Activity;
import android.view.WindowManager;

final class OrganizationAdministrationPrivacy {
    private OrganizationAdministrationPrivacy() {
        throw new AssertionError("No instances.");
    }

    static void protect(Activity activity) {
        activity.getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SECURE);
    }
}
