package br.com.tresvtintas.mobile.feature.laborquote;

import android.text.InputFilter;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import br.com.tresvtintas.mobile.core.customer.CustomerException;
import br.com.tresvtintas.mobile.core.customer.CustomerQuery;
import br.com.tresvtintas.mobile.core.customer.CustomerSummary;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

final class LaborQuoteCustomerPicker {
    private static final int LOOKUP_PAGE_SIZE = 100;
    private final AppCompatActivity activity;
    private final LaborQuoteEditorView editorView;
    private final Consumer<CustomerSummary> onSelected;
    private long generation;
    private boolean lookupInProgress;

    LaborQuoteCustomerPicker(
            AppCompatActivity activity,
            LaborQuoteEditorView editorView,
            Consumer<CustomerSummary> onSelected) {
        this.activity = activity;
        this.editorView = editorView;
        this.onSelected = onSelected;
    }

    void choose(Optional<LaborQuoteFeatureRuntime> runtime) {
        if (runtime.isEmpty() || lookupInProgress) {
            return;
        }
        EditText input = new EditText(activity);
        input.setSingleLine(true);
        input.setHint(R.string.labor_quote_customer_search_hint);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(80)});
        new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.labor_quote_search_customer)
                .setView(input)
                .setPositiveButton(
                        R.string.labor_quote_search,
                        (dialog, which) -> lookup(runtime.orElseThrow(), text(input)))
                .setNegativeButton(R.string.labor_quote_cancel, null)
                .show();
    }

    void stop() {
        generation++;
        lookupInProgress = false;
    }

    private void lookup(LaborQuoteFeatureRuntime runtime, String search) {
        lookupInProgress = true;
        generation++;
        long operation = generation;
        editorView.progress(true);
        editorView.setEnabled(false, true);
        runtime.workerExecutor().execute(() -> {
            try {
                List<CustomerSummary> found = runtime.customerRepository().page(
                        new CustomerQuery(Optional.of(search), LOOKUP_PAGE_SIZE),
                        Optional.empty()).items();
                activity.runOnUiThread(() -> show(operation, found));
            } catch (CustomerException exception) {
                activity.runOnUiThread(() -> fail(operation));
            }
        });
    }

    private void show(long operation, List<CustomerSummary> customers) {
        if (!finish(operation)) {
            return;
        }
        if (customers.isEmpty()) {
            editorView.message(R.string.labor_quote_customer_empty);
            return;
        }
        new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.labor_quote_select_customer)
                .setItems(
                        customers.stream().map(CustomerSummary::name).toArray(String[]::new),
                        (dialog, which) -> onSelected.accept(customers.get(which)))
                .setNegativeButton(R.string.labor_quote_cancel, null)
                .show();
    }

    private void fail(long operation) {
        if (finish(operation)) {
            editorView.message(R.string.labor_quote_failure);
        }
    }

    private boolean finish(long operation) {
        if (operation != generation) {
            return false;
        }
        lookupInProgress = false;
        editorView.progress(false);
        editorView.setEnabled(true, true);
        editorView.clearMessage();
        return true;
    }

    private static String text(EditText field) {
        return field.getText() == null ? "" : field.getText().toString();
    }
}
