package br.com.tresvtintas.mobile.feature.customer;

import android.app.Activity;
import android.view.WindowManager;

final class CustomerPrivacy {
    private CustomerPrivacy() {
        throw new AssertionError("No instances.");
    }

    static void protect(Activity activity) {
        activity.getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SECURE);
    }
}
