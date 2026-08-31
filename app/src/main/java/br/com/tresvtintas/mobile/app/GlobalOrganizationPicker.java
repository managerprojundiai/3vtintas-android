package br.com.tresvtintas.mobile.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import br.com.tresvtintas.mobile.core.corporatefinance.CorporateFinanceOrganization;
import br.com.tresvtintas.mobile.core.corporatefinance.CorporateFinanceOrganizationPage;
import br.com.tresvtintas.mobile.core.corporatefinance.CorporateFinanceOrganizationRepository;
import br.com.tresvtintas.mobile.core.finance.FinanceException;
import br.com.tresvtintas.mobile.core.model.OrganizationMembershipRole;
import br.com.tresvtintas.mobile.core.model.OrganizationScope;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Loads the protected organization directory for a master account before showing the picker.
 * Global bootstrap intentionally omits the organization list, so this keeps selection explicit
 * without weakening the server authorization contract.
 */
final class GlobalOrganizationPicker {
    private static final int PAGE_SIZE = 100;
    private static final int MAXIMUM_PAGES = 20;

    private GlobalOrganizationPicker() {
        throw new AssertionError("No instances.");
    }

    static void show(
            Context context,
            CorporateFinanceOrganizationRepository repository,
            Executor worker,
            Executor main,
            Consumer<List<OrganizationScope>> onLoaded) {
        if (context == null
                || repository == null
                || worker == null
                || main == null
                || onLoaded == null) {
            return;
        }
        Dialog loading = new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.shell_global_store_loading_title)
                .setMessage(R.string.shell_global_store_loading_message)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        loading.setCanceledOnTouchOutside(false);
        loading.show();
        worker.execute(() -> {
            try {
                List<OrganizationScope> organizations = load(repository);
                main.execute(() -> {
                    dismiss(loading);
                    if (organizations.isEmpty()) {
                        showMessage(context,
                                R.string.shell_global_store_empty_title,
                                R.string.shell_global_store_empty_message);
                        return;
                    }
                    onLoaded.accept(organizations);
                });
            } catch (FinanceException | IllegalStateException failure) {
                main.execute(() -> {
                    dismiss(loading);
                    showMessage(context,
                            R.string.shell_global_store_error_title,
                            R.string.shell_global_store_error_message);
                });
            }
        });
    }

    private static List<OrganizationScope> load(
            CorporateFinanceOrganizationRepository repository)
            throws FinanceException {
        List<OrganizationScope> result = new ArrayList<>();
        Optional<String> cursor = Optional.empty();
        for (int pageNumber = 0; pageNumber < MAXIMUM_PAGES; pageNumber++) {
            CorporateFinanceOrganizationPage page = repository.page(
                    Optional.empty(),
                    cursor,
                    PAGE_SIZE);
            for (CorporateFinanceOrganization organization : page.items()) {
                result.add(new OrganizationScope(
                        organization.id(),
                        organization.name(),
                        "global-" + organization.id(),
                        OrganizationMembershipRole.OWNER));
            }
            Optional<String> next = page.nextCursor();
            if (next.isEmpty()) {
                return List.copyOf(result);
            }
            if (next.equals(cursor)) {
                throw new IllegalStateException("Organization directory cursor did not advance.");
            }
            cursor = next;
        }
        throw new IllegalStateException("Organization directory exceeded the safe page limit.");
    }

    private static void dismiss(Dialog dialog) {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    private static void showMessage(Context context, int title, int message) {
        if (context instanceof Activity activity
                && (activity.isFinishing() || activity.isDestroyed())) {
            return;
        }
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
