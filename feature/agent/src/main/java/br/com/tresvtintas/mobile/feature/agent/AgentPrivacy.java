package br.com.tresvtintas.mobile.feature.agent;

import android.app.Activity;
import android.view.WindowManager;

final class AgentPrivacy {
    private AgentPrivacy() {
        throw new AssertionError("No instances.");
    }

    static void protect(Activity activity) {
        if (activity == null) {
            throw new IllegalArgumentException(
                    "Agent activity is required.");
        }
        activity.getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SECURE);
    }
}
