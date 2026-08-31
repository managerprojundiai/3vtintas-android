package br.com.tresvtintas.mobile.feature.accountaccess;

import android.app.Activity;
import android.view.WindowManager;

final class AccountAccessPrivacy {
    private AccountAccessPrivacy() {
    }

    static void protect(Activity activity) {
        activity.getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SECURE);
    }
}
