package br.com.tresvtintas.mobile.app;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import br.com.tresvtintas.mobile.app.databinding.MainShellMoreSheetBinding;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

final class ShellMoreSheet {
    @FunctionalInterface
    interface Listener {
        void onActionSelected(ShellAction action);
    }

    private final Context context;
    private final Listener listener;
    private Optional<BottomSheetDialog> dialog = Optional.empty();

    ShellMoreSheet(Context context, Listener listener) {
        if (context == null || listener == null) {
            throw new IllegalArgumentException(
                    "Bottom sheet context and listener are required.");
        }
        this.context = context;
        this.listener = listener;
    }

    void show(List<ShellAction> actions) {
        dismiss();
        MainShellMoreSheetBinding binding =
                MainShellMoreSheetBinding.inflate(
                        android.view.LayoutInflater.from(context));
        ShellActionAdapter adapter = new ShellActionAdapter(action -> {
            dismiss();
            listener.onActionSelected(action);
        }, true);
        adapter.submit(actions);
        binding.shellMoreActions.setLayoutManager(
                new LinearLayoutManager(context));
        binding.shellMoreActions.setAdapter(adapter);
        binding.shellMoreSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence value, int start, int count, int after) {
                // No pre-change action.
            }

            @Override
            public void onTextChanged(
                    CharSequence value, int start, int before, int count) {
                List<ShellAction> filtered = filter(
                        actions, value == null ? "" : value.toString());
                adapter.submit(filtered);
                binding.shellMoreEmpty.setVisibility(
                        filtered.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable value) {
                // Filtering happens in onTextChanged.
            }
        });
        BottomSheetDialog displayedDialog = new BottomSheetDialog(context);
        displayedDialog.setContentView(binding.getRoot());
        displayedDialog.setOnDismissListener(
                ignored -> dialog = Optional.empty());
        dialog = Optional.of(displayedDialog);
        displayedDialog.show();
        View sheet = displayedDialog.findViewById(
                com.google.android.material.R.id.design_bottom_sheet);
        if (sheet != null) {
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(sheet);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
        }
    }

    void dismiss() {
        dialog.ifPresent(BottomSheetDialog::dismiss);
        dialog = Optional.empty();
    }

    private List<ShellAction> filter(
            List<ShellAction> actions,
            String rawSearch) {
        String search = normalized(rawSearch);
        if (search.isEmpty()) {
            return actions;
        }
        return actions.stream()
                .filter(action -> normalized(
                        context.getString(action.titleResource())
                                + " "
                                + context.getString(action.descriptionResource()))
                        .contains(search))
                .collect(Collectors.toList());
    }

    private static String normalized(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.forLanguageTag("pt-BR"))
                .trim();
    }
}
