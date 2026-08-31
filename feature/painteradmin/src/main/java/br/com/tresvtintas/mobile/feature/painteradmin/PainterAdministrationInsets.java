package br.com.tresvtintas.mobile.feature.painteradmin;

import android.view.View;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

final class PainterAdministrationInsets {
    private PainterAdministrationInsets() {
        throw new AssertionError("No instances.");
    }

    static void applySystemBars(View root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }
}
