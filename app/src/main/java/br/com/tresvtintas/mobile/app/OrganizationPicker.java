package br.com.tresvtintas.mobile.app;

import android.content.Context;
import br.com.tresvtintas.mobile.core.model.OrganizationScope;
import br.com.tresvtintas.mobile.feature.shell.ShellAccessState;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.List;
import java.util.function.LongConsumer;

final class OrganizationPicker {
    private OrganizationPicker() {
        throw new AssertionError("No instances.");
    }

    static void show(
            Context context,
            ShellAccessState access,
            LongConsumer onSelected) {
        if (context == null || access == null || onSelected == null) {
            return;
        }
        List<OrganizationScope> organizations =
                access.bootstrap().authorization().organizations();
        if (organizations.isEmpty()) {
            return;
        }
        String[] labels = organizations.stream()
                .map(OrganizationScope::name)
                .toArray(String[]::new);
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.shell_store_dialog_title)
                .setItems(labels, (dialog, index) ->
                        onSelected.accept(organizations.get(index).id()))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
