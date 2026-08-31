package br.com.tresvtintas.mobile.feature.accountaccess;

import android.view.View;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

final class AccountAccessInsets {
    private AccountAccessInsets() {
    }

    static void applySystemBars(View view) {
        ViewCompat.setOnApplyWindowInsetsListener(
                view,
                (target, insets) -> {
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
