package br.com.tresvtintas.mobile.feature.team;

import android.app.Activity;
import android.view.WindowManager;

final class TeamPrivacy {
    private TeamPrivacy() {
    }

    static void protect(Activity activity) {
        activity.getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SECURE);
    }
}
