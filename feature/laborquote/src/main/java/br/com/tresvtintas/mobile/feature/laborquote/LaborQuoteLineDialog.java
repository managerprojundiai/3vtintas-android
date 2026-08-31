package br.com.tresvtintas.mobile.feature.laborquote;

import android.content.Context;
import androidx.appcompat.app.AlertDialog;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteDraftLine;
import br.com.tresvtintas.mobile.feature.laborquote.databinding.LaborQuoteDialogLineBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.math.BigDecimal;

final class LaborQuoteLineDialog {
    private LaborQuoteLineDialog() {
    }

    static void show(
            Context context,
            LaborQuoteDraftLine existing,
            Listener listener) {
        LaborQuoteDialogLineBinding binding = LaborQuoteDialogLineBinding.inflate(
                android.view.LayoutInflater.from(context));
        if (existing != null) {
            binding.laborQuoteLineDescription.setText(existing.description());
            binding.laborQuoteLineQuantity.setText(existing.quantity().toPlainString());
            binding.laborQuoteLineUnit.setText(existing.unit());
            binding.laborQuoteLineUnitPrice.setText(existing.unitPrice().toPlainString());
        }
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
                .setTitle(existing == null
                        ? R.string.labor_quote_add_service
                        : R.string.labor_quote_edit_service)
                .setView(binding.getRoot())
                .setPositiveButton(
                        existing == null
                                ? R.string.labor_quote_add
                                : R.string.labor_quote_update,
                        null)
                .setNegativeButton(R.string.labor_quote_cancel, null);
        if (existing != null) {
            builder.setNeutralButton(R.string.labor_quote_remove_service, (dialog, which) ->
                    listener.remove());
        }
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    try {
                        LaborQuoteDraftLine line = new LaborQuoteDraftLine(
                                text(binding.laborQuoteLineDescription),
                                decimal(binding.laborQuoteLineQuantity),
                                text(binding.laborQuoteLineUnit),
                                decimal(binding.laborQuoteLineUnitPrice));
                        listener.save(line);
                        dialog.dismiss();
                    } catch (IllegalArgumentException exception) {
                        binding.laborQuoteLineDescription.setError(
                                context.getString(R.string.labor_quote_invalid));
                    }
                }));
        dialog.show();
    }

    private static BigDecimal decimal(android.widget.EditText field) {
        return new BigDecimal(text(field).replace(',', '.'));
    }

    private static String text(android.widget.EditText field) {
        return field.getText() == null ? "" : field.getText().toString();
    }

    interface Listener {
        void save(LaborQuoteDraftLine line);

        void remove();
    }
}
