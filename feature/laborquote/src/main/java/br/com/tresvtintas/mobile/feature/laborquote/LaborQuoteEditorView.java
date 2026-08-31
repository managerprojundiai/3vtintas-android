package br.com.tresvtintas.mobile.feature.laborquote;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import androidx.annotation.StringRes;
import br.com.tresvtintas.mobile.core.customer.CustomerSummary;
import br.com.tresvtintas.mobile.feature.laborquote.databinding.LaborQuoteActivityEditBinding;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

final class LaborQuoteEditorView {
    private final Context context;
    private final LaborQuoteActivityEditBinding binding;
    private final ArrayAdapter<String> itemAdapter;

    LaborQuoteEditorView(Context context, LaborQuoteActivityEditBinding binding) {
        this.context = context;
        this.binding = binding;
        itemAdapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_list_item_1,
                new ArrayList<>());
        binding.laborQuoteItems.setAdapter(itemAdapter);
    }

    void observeDiscount(Runnable onChanged) {
        binding.laborQuoteDiscount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence value, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence value, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable value) {
                onChanged.run();
            }
        });
    }

    void restoreText(String title, String notes, String discount) {
        binding.laborQuoteTitle.setText(title);
        binding.laborQuoteNotes.setText(notes);
        binding.laborQuoteDiscount.setText(discount);
    }

    void render(Optional<CustomerSummary> customer, LaborQuoteEditorModel editor) {
        binding.laborQuoteCustomer.setText(customer
                .map(CustomerSummary::name)
                .orElse(context.getString(R.string.labor_quote_select_customer)));
        itemAdapter.clear();
        editor.lines().forEach(line -> itemAdapter.add(
                line.description()
                        + "\n"
                        + line.quantity().toPlainString()
                        + " "
                        + line.unit()
                        + " × R$ "
                        + line.unitPrice().toPlainString()
                        + " = R$ "
                        + line.total().toPlainString()
                        + " • "
                        + context.getString(R.string.labor_quote_item_remove_hint)));
        BigDecimal discount = discountOrZero();
        binding.laborQuoteEstimate.setText(context.getString(
                R.string.labor_quote_total_estimate,
                editor.subtotal().toPlainString(),
                discount.toPlainString(),
                editor.total(discount).toPlainString()));
    }

    void setEnabled(boolean enabled, boolean customerCanChange) {
        binding.laborQuoteTitle.setEnabled(enabled);
        binding.laborQuoteNotes.setEnabled(enabled);
        binding.laborQuoteDiscount.setEnabled(enabled);
        binding.laborQuoteAddService.setEnabled(enabled);
        binding.laborQuoteSave.setEnabled(enabled);
        binding.laborQuoteCustomer.setEnabled(enabled && customerCanChange);
    }

    void progress(boolean visible) {
        binding.laborQuoteEditProgress.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    }

    void message(@StringRes int resource) {
        binding.laborQuoteEditError.setText(resource);
    }

    void clearMessage() {
        binding.laborQuoteEditError.setText("");
    }

    String title() {
        return text(binding.laborQuoteTitle);
    }

    String notes() {
        return text(binding.laborQuoteNotes);
    }

    String discountText() {
        return text(binding.laborQuoteDiscount);
    }

    BigDecimal discount() {
        String value = discountText().trim().replace(',', '.');
        return value.isEmpty() ? BigDecimal.ZERO : new BigDecimal(value);
    }

    private BigDecimal discountOrZero() {
        try {
            return discount().setScale(2);
        } catch (IllegalArgumentException exception) {
            return BigDecimal.ZERO.setScale(2);
        }
    }

    private static String text(android.widget.EditText field) {
        return field.getText() == null ? "" : field.getText().toString();
    }
}
