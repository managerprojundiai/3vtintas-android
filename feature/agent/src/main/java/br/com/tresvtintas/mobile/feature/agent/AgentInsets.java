package br.com.tresvtintas.mobile.feature.agent;

import android.view.View;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

final class AgentInsets {
    private AgentInsets() {
        throw new AssertionError("No instances.");
    }

    static void applySystemBars(View root) {
        if (root == null) {
            throw new IllegalArgumentException(
                    "Agent root view is required.");
        }
        int left = root.getPaddingLeft();
        int top = root.getPaddingTop();
        int right = root.getPaddingRight();
        int bottom = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars());
            view.setPadding(
                    left + bars.left,
                    top + bars.top,
                    right + bars.right,
                    bottom + bars.bottom);
            return insets;
        });
    }
}
