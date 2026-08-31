package br.com.tresvtintas.mobile.feature.quote;

import android.app.Activity;
import android.text.InputFilter;
import android.widget.EditText;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteException;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteFailureKind;
import br.com.tresvtintas.mobile.core.quote.MaterialQuotePricingSelection;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteRepository;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteTintColor;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteTintColorPage;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteTintConfiguration;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;

final class MaterialQuoteTintConfigurator {
    private static final int MINIMUM_COLOR_SEARCH_LENGTH = 2;
    private static final int COLOR_LIMIT = 30;
    private final Activity activity;
    private final MaterialQuoteRepository repository;
    private final Executor executor;
    private final Listener listener;
    private long generation;

    MaterialQuoteTintConfigurator(
            Activity activity,
            MaterialQuoteRepository repository,
            Executor executor,
            Listener listener) {
        this.activity = activity;
        this.repository = repository;
        this.executor = executor;
        this.listener = listener;
    }

    void start(long organizationId, MaterialQuotePricingSelection pricing) {
        generation++;
        long operation = generation;
        listener.onBusyChanged(true);
        executor.execute(() -> {
            try {
                List<MaterialQuoteTintConfiguration> values =
                        repository.tintConfigurations(organizationId, pricing);
                activity.runOnUiThread(() -> showSources(
                        operation, organizationId, pricing, values));
            } catch (MaterialQuoteException exception) {
                activity.runOnUiThread(() -> fail(operation, exception.kind()));
            }
        });
    }

    void cancel() {
        generation++;
    }

    private void showSources(
            long operation,
            long organizationId,
            MaterialQuotePricingSelection pricing,
            List<MaterialQuoteTintConfiguration> values) {
        if (!current(operation)) {
            return;
        }
        listener.onBusyChanged(false);
        List<String> sources = MaterialQuoteTintOptions.sources(values);
        if (sources.isEmpty()) {
            listener.onFailure(MaterialQuoteFailureKind.TINT_MAPPING_PENDING);
            return;
        }
        String[] labels = sources.stream()
                .map(MaterialQuoteTintConfigurator::sourceLabel)
                .toArray(String[]::new);
        new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.quote_tint_source_title)
                .setItems(labels, (dialog, position) -> showLines(
                        operation,
                        organizationId,
                        pricing,
                        values,
                        sources.get(position)))
                .setNegativeButton(R.string.quote_cancel, null)
                .show();
    }

    private void showLines(
            long operation,
            long organizationId,
            MaterialQuotePricingSelection pricing,
            List<MaterialQuoteTintConfiguration> values,
            String source) {
        List<String> options = MaterialQuoteTintOptions.lines(values, source);
        showOptions(
                R.string.quote_tint_line_title,
                options,
                selected -> showFinishes(
                        operation,
                        organizationId,
                        pricing,
                        values,
                        source,
                        selected));
    }

    private void showFinishes(
            long operation,
            long organizationId,
            MaterialQuotePricingSelection pricing,
            List<MaterialQuoteTintConfiguration> values,
            String source,
            String line) {
        List<String> options = MaterialQuoteTintOptions.finishes(
                values, source, line);
        showOptions(
                R.string.quote_tint_finish_title,
                options,
                selected -> showPackages(
                        operation,
                        organizationId,
                        pricing,
                        values,
                        source,
                        line,
                        selected));
    }

    private void showPackages(
            long operation,
            long organizationId,
            MaterialQuotePricingSelection pricing,
            List<MaterialQuoteTintConfiguration> values,
            String source,
            String line,
            String finish) {
        List<String> options = MaterialQuoteTintOptions.packages(
                values, source, line, finish);
        showOptions(
                R.string.quote_tint_package_title,
                options,
                selected -> askColorSearch(
                        operation,
                        organizationId,
                        pricing,
                        MaterialQuoteTintOptions.configuration(
                                values, source, line, finish, selected)));
    }

    private void showOptions(
            int title,
            List<String> options,
            SelectionListener selection) {
        if (options.isEmpty()) {
            listener.onFailure(MaterialQuoteFailureKind.TINT_MAPPING_PENDING);
            return;
        }
        new MaterialAlertDialogBuilder(activity)
                .setTitle(title)
                .setItems(options.toArray(new String[0]),
                        (dialog, position) -> selection.onSelected(
                                options.get(position)))
                .setNegativeButton(R.string.quote_cancel, null)
                .show();
    }

    private void askColorSearch(
            long operation,
            long organizationId,
            MaterialQuotePricingSelection pricing,
            MaterialQuoteTintConfiguration configuration) {
        EditText input = new EditText(activity);
        input.setSingleLine(true);
        input.setHint(R.string.quote_tint_color_hint);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(80)});
        var dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.quote_tint_color_title)
                .setView(input)
                .setPositiveButton(R.string.quote_search_button, null)
                .setNegativeButton(R.string.quote_cancel, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(
                android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                    String search = input.getText() == null
                            ? ""
                            : input.getText().toString().trim();
                    if (search.length() < MINIMUM_COLOR_SEARCH_LENGTH) {
                        input.setError(activity.getString(
                                R.string.quote_tint_color_minimum));
                        return;
                    }
                    dialog.dismiss();
                    searchColors(
                            operation,
                            organizationId,
                            pricing,
                            configuration,
                            search);
                }));
        dialog.show();
    }

    private void searchColors(
            long operation,
            long organizationId,
            MaterialQuotePricingSelection pricing,
            MaterialQuoteTintConfiguration configuration,
            String search) {
        listener.onBusyChanged(true);
        executor.execute(() -> {
            try {
                MaterialQuoteTintColorPage page = repository.tintColors(
                        organizationId,
                        pricing,
                        configuration,
                        search,
                        COLOR_LIMIT);
                activity.runOnUiThread(() -> showColors(operation, page));
            } catch (MaterialQuoteException exception) {
                activity.runOnUiThread(() -> fail(operation, exception.kind()));
            }
        });
    }

    private void showColors(long operation, MaterialQuoteTintColorPage page) {
        if (!current(operation)) {
            return;
        }
        listener.onBusyChanged(false);
        if (page.items().isEmpty()) {
            listener.onFailure(MaterialQuoteFailureKind.PRICE_NOT_AVAILABLE);
            return;
        }
        NumberFormat currency = NumberFormat.getCurrencyInstance(
                new Locale("pt", "BR"));
        String[] labels = page.items().stream()
                .map(value -> value.colorName()
                        + " — " + value.productName()
                        + value.productSku().map(sku -> " • SKU " + sku).orElse("")
                        + "\n" + value.configuration().lineName()
                        + " • " + value.configuration().finishName()
                        + " • " + value.configuration().packageName()
                        + " • " + currency.format(value.amount()))
                .toArray(String[]::new);
        new MaterialAlertDialogBuilder(activity)
                .setTitle(page.hasMore()
                        ? R.string.quote_tint_results_limited
                        : R.string.quote_tint_results_title)
                .setItems(labels, (dialog, position) ->
                        listener.onSelected(page.items().get(position)))
                .setNegativeButton(R.string.quote_cancel, null)
                .show();
    }

    private void fail(long operation, MaterialQuoteFailureKind kind) {
        if (current(operation)) {
            listener.onBusyChanged(false);
            listener.onFailure(kind);
        }
    }

    private boolean current(long operation) {
        return operation == generation && !activity.isFinishing();
    }

    private static String sourceLabel(String value) {
        return "LKC".equals(value) ? "Lukscolor" : "Corimo";
    }

    interface Listener {
        void onBusyChanged(boolean busy);

        void onSelected(MaterialQuoteTintColor color);

        void onFailure(MaterialQuoteFailureKind kind);
    }

    @FunctionalInterface
    private interface SelectionListener {
        void onSelected(String value);
    }
}
