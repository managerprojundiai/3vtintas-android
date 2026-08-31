package br.com.tresvtintas.mobile.feature.laborquote;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteException;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuotePage;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteQuery;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteSummary;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteView;
import br.com.tresvtintas.mobile.feature.laborquote.databinding.LaborQuoteActivityListBinding;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class LaborQuoteListActivity extends AppCompatActivity {
    private final List<LaborQuoteSummary> quotes = new ArrayList<>();
    private LaborQuoteActivityListBinding binding;
    private ArrayAdapter<String> adapter;
    private Optional<LaborQuoteFeatureRuntime> runtime = Optional.empty();
    private Optional<String> nextCursor = Optional.empty();
    private long generation;
    private boolean loading;
    private LaborQuoteView view = LaborQuoteView.ACTIVE;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        LaborQuotePrivacy.protect(this);
        EdgeToEdge.enable(this);
        binding = LaborQuoteActivityListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                new ArrayList<>());
        binding.laborQuoteList.setAdapter(adapter);
        binding.laborQuoteToolbar.setNavigationOnClickListener(ignored -> finish());
        binding.laborQuoteSearchAction.setOnClickListener(ignored -> loadPage(true));
        binding.laborQuoteViewActive.setChecked(true);
        binding.laborQuoteViewGroup.addOnButtonCheckedListener(
                (group, checkedId, isChecked) -> {
                    if (!isChecked) {
                        return;
                    }
                    if (checkedId == R.id.labor_quote_view_history) {
                        view = LaborQuoteView.HISTORY;
                    } else if (checkedId == R.id.labor_quote_view_all) {
                        view = LaborQuoteView.ALL;
                    } else {
                        view = LaborQuoteView.ACTIVE;
                    }
                    if (runtime.isPresent()) {
                        generation++;
                        loading = false;
                        loadPage(true);
                    }
                });
        binding.laborQuoteLoadMore.setOnClickListener(ignored -> loadPage(false));
        binding.laborQuoteNew.setOnClickListener(ignored -> startActivity(
                new Intent(this, LaborQuoteEditActivity.class)));
        binding.laborQuoteList.setOnItemClickListener((parent, view, position, id) ->
                startActivity(LaborQuoteDetailActivity.intent(
                        this,
                        quotes.get(position).id())));
    }

    @Override
    protected void onStart() {
        super.onStart();
        runtime = runtime();
        binding.laborQuoteNew.setVisibility(
                runtime.map(LaborQuoteFeatureRuntime::draftWriteAllowed).orElse(false)
                        ? View.VISIBLE
                        : View.GONE);
        loadPage(true);
    }

    @Override
    protected void onStop() {
        generation++;
        loading = false;
        runtime = Optional.empty();
        super.onStop();
    }

    private void loadPage(boolean replace) {
        if (runtime.isEmpty() || loading || (!replace && nextCursor.isEmpty())) {
            return;
        }
        loading = true;
        generation++;
        long operation = generation;
        binding.laborQuoteProgress.setVisibility(View.VISIBLE);
        binding.laborQuoteNotice.setText("");
        binding.laborQuoteSearchAction.setEnabled(false);
        binding.laborQuoteLoadMore.setEnabled(false);
        String search = binding.laborQuoteSearchInput.getText() == null
                ? ""
                : binding.laborQuoteSearchInput.getText().toString();
        Optional<String> cursor = replace ? Optional.empty() : nextCursor;
        LaborQuoteFeatureRuntime available = runtime.orElseThrow();
        available.workerExecutor().execute(() -> {
            try {
                LaborQuotePage page = available.quoteRepository().page(
                        new LaborQuoteQuery(
                                Optional.of(search),
                                Optional.empty(),
                                view,
                                LaborQuoteQuery.initial().pageSize()),
                        cursor);
                runOnUiThread(() -> showPage(operation, page, replace));
            } catch (LaborQuoteException exception) {
                runOnUiThread(() -> showFailure(operation));
            }
        });
    }

    private void showPage(long operation, LaborQuotePage page, boolean replace) {
        if (operation != generation) {
            return;
        }
        loading = false;
        binding.laborQuoteProgress.setVisibility(View.INVISIBLE);
        binding.laborQuoteSearchAction.setEnabled(true);
        if (replace) {
            quotes.clear();
        }
        quotes.addAll(page.items());
        nextCursor = page.nextCursor();
        adapter.clear();
        NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        quotes.forEach(quote -> adapter.add(
                quote.title()
                        + "\n"
                        + quote.customer().name()
                        + " • "
                        + currency.format(quote.total())
                        + " • "
                        + statusLabel(quote)));
        binding.laborQuoteLoadMore.setVisibility(
                nextCursor.isPresent() ? View.VISIBLE : View.GONE);
        binding.laborQuoteLoadMore.setEnabled(nextCursor.isPresent());
        if (quotes.isEmpty()) {
            binding.laborQuoteNotice.setText(switch (view) {
                case ACTIVE -> R.string.labor_quote_active_empty;
                case HISTORY -> R.string.labor_quote_history_empty;
                case ALL -> R.string.labor_quote_empty;
            });
        }
    }

    private String statusLabel(LaborQuoteSummary quote) {
        return getString(LaborQuoteUi.statusLabel(quote.status()));
    }

    private void showFailure(long operation) {
        if (operation == generation) {
            loading = false;
            binding.laborQuoteProgress.setVisibility(View.INVISIBLE);
            binding.laborQuoteSearchAction.setEnabled(true);
            binding.laborQuoteLoadMore.setEnabled(nextCursor.isPresent());
            binding.laborQuoteNotice.setText(R.string.labor_quote_failure);
        }
    }

    private Optional<LaborQuoteFeatureRuntime> runtime() {
        if (getApplication() instanceof LaborQuoteRuntimeProvider provider) {
            return provider.laborQuoteRuntime();
        }
        return Optional.empty();
    }
}
