package br.com.tresvtintas.mobile.feature.quote;

import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import androidx.recyclerview.widget.LinearLayoutManager;
import br.com.tresvtintas.mobile.core.catalog.CatalogProduct;
import br.com.tresvtintas.mobile.feature.quote.databinding.QuoteProductPickerBinding;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class MaterialQuoteProductPicker {
    private static final int MINIMUM_SEARCH_LENGTH = 2;

    interface Listener {
        void onSearch(String search);

        void onAdd(CatalogProduct product, String quantity);
    }

    private final Activity activity;
    private final Listener listener;
    private Optional<BottomSheetDialog> dialog = Optional.empty();
    private Optional<QuoteProductPickerBinding> binding = Optional.empty();
    private Optional<CatalogProduct> selected = Optional.empty();

    MaterialQuoteProductPicker(Activity activity, Listener listener) {
        this.activity = activity;
        this.listener = listener;
    }

    void show() {
        dismiss();
        QuoteProductPickerBinding current = QuoteProductPickerBinding.inflate(
                activity.getLayoutInflater());
        MaterialQuoteProductAdapter adapter = new MaterialQuoteProductAdapter(
                this::select);
        current.quoteProductPickerResults.setLayoutManager(
                new LinearLayoutManager(activity));
        current.quoteProductPickerResults.setAdapter(adapter);
        current.quoteProductPickerSearchButton.setOnClickListener(
                ignored -> search(current));
        current.quoteProductPickerSearch.setOnEditorActionListener(
                (view, actionId, event) -> {
                    if (actionId != EditorInfo.IME_ACTION_SEARCH) {
                        return false;
                    }
                    search(current);
                    return true;
                });
        current.quoteProductPickerSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence value, int start, int count, int after) {
                // No pre-change action.
            }

            @Override
            public void onTextChanged(
                    CharSequence value, int start, int before, int count) {
                current.quoteProductPickerSearchContainer.setError(null);
            }

            @Override
            public void afterTextChanged(Editable value) {
                // Search is explicit to avoid surprising network calls.
            }
        });
        current.quoteProductPickerAdd.setOnClickListener(ignored ->
                selected.ifPresent(product -> {
                    listener.onAdd(
                            product,
                            text(current.quoteProductPickerQuantity));
                    dismiss();
                }));
        BottomSheetDialog displayed = new BottomSheetDialog(activity);
        displayed.setContentView(current.getRoot());
        displayed.setOnDismissListener(ignored -> {
            dialog = Optional.empty();
            binding = Optional.empty();
            selected = Optional.empty();
        });
        dialog = Optional.of(displayed);
        binding = Optional.of(current);
        displayed.show();
        View sheet = displayed.findViewById(
                com.google.android.material.R.id.design_bottom_sheet);
        if (sheet != null) {
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(sheet);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
        }
        current.quoteProductPickerSearch.requestFocus();
    }

    void renderLoading() {
        binding.ifPresent(current -> {
            current.quoteProductPickerProgress.setVisibility(View.VISIBLE);
            current.quoteProductPickerStatus.setText(R.string.quote_loading);
            current.quoteProductPickerSearchButton.setEnabled(false);
        });
    }

    void renderResults(List<CatalogProduct> products) {
        binding.ifPresent(current -> {
            current.quoteProductPickerProgress.setVisibility(View.INVISIBLE);
            current.quoteProductPickerSearchButton.setEnabled(true);
            MaterialQuoteProductAdapter adapter =
                    (MaterialQuoteProductAdapter) current
                            .quoteProductPickerResults.getAdapter();
            adapter.submitList(List.copyOf(products));
            current.quoteProductPickerStatus.setText(products.isEmpty()
                    ? activity.getString(R.string.quote_product_empty)
                    : activity.getResources().getQuantityString(
                            R.plurals.quote_product_results,
                            products.size(),
                            products.size()));
        });
    }

    void renderFailure() {
        binding.ifPresent(current -> {
            current.quoteProductPickerProgress.setVisibility(View.INVISIBLE);
            current.quoteProductPickerSearchButton.setEnabled(true);
            current.quoteProductPickerStatus.setText(R.string.quote_failure);
        });
    }

    void dismiss() {
        dialog.ifPresent(BottomSheetDialog::dismiss);
        dialog = Optional.empty();
        binding = Optional.empty();
        selected = Optional.empty();
    }

    private void search(QuoteProductPickerBinding current) {
        String search = text(current.quoteProductPickerSearch).trim();
        if (search.length() < MINIMUM_SEARCH_LENGTH) {
            current.quoteProductPickerSearchContainer.setError(
                    activity.getString(R.string.quote_product_search_minimum));
            return;
        }
        current.quoteProductPickerSearchContainer.setError(null);
        listener.onSearch(search);
    }

    private void select(CatalogProduct product) {
        binding.ifPresent(current -> {
            selected = Optional.of(product);
            NumberFormat currency = NumberFormat.getCurrencyInstance(
                    Locale.forLanguageTag("pt-BR"));
            current.quoteProductPickerSelected.setVisibility(View.VISIBLE);
            current.quoteProductPickerSelectedName.setText(product.name());
            current.quoteProductPickerSelectedPrice.setText(product.price()
                    .map(currency::format)
                    .orElse(activity.getString(R.string.quote_price_unavailable)));
            current.quoteProductPickerAdd.setEnabled(product.price().isPresent());
            current.quoteProductPickerQuantity.setText(
                    R.string.quote_default_quantity);
        });
    }

    private static String text(android.widget.TextView field) {
        return field.getText() == null ? "" : field.getText().toString();
    }
}
