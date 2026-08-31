package br.com.tresvtintas.mobile.feature.dashboard;

import android.view.View;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

final class DashboardInsets {
    private DashboardInsets() {
    }

    static void applySystemBars(View view) {
        ViewCompat.setOnApplyWindowInsetsListener(view, (target, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars());
            target.setPadding(
                    bars.left,
                    bars.top,
                    bars.right,
                    bars.bottom);
            return insets;
        });
    }
}
