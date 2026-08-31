package br.com.tresvtintas.mobile.feature.quote;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import br.com.tresvtintas.mobile.core.quote.MaterialQuotePage;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteException;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteQuery;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteSummary;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteView;
import br.com.tresvtintas.mobile.feature.quote.databinding.QuoteActivityListBinding;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class MaterialQuoteListActivity extends AppCompatActivity {
    private final List<MaterialQuoteSummary> quotes = new ArrayList<>();
    private QuoteActivityListBinding binding;
    private ArrayAdapter<String> adapter;
    private Optional<MaterialQuoteFeatureRuntime> runtime = Optional.empty();
    private Optional<String> nextCursor = Optional.empty();
    private long generation;
    private boolean loading;
    private MaterialQuoteView view = MaterialQuoteView.ACTIVE;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        MaterialQuotePrivacy.protect(this);
        EdgeToEdge.enable(this);
        binding = QuoteActivityListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                new ArrayList<>());
        binding.quoteList.setAdapter(adapter);
        binding.quoteToolbar.setNavigationOnClickListener(ignored -> finish());
        binding.quoteSearchAction.setOnClickListener(ignored -> loadFirstPage());
        binding.quoteViewActive.setChecked(true);
        binding.quoteViewGroup.addOnButtonCheckedListener(
                (group, checkedId, isChecked) -> {
                    if (!isChecked) {
                        return;
                    }
                    if (checkedId == R.id.quote_view_history) {
                        view = MaterialQuoteView.HISTORY;
                    } else if (checkedId == R.id.quote_view_all) {
                        view = MaterialQuoteView.ALL;
                    } else {
                        view = MaterialQuoteView.ACTIVE;
                    }
                    if (runtime.isPresent()) {
                        generation++;
                        loading = false;
                        loadFirstPage();
                    }
                });
        binding.quoteLoadMore.setOnClickListener(ignored -> loadNextPage());
        binding.quoteNew.setOnClickListener(ignored -> startActivity(
                new Intent(this, MaterialQuoteEditActivity.class)));
        binding.quoteList.setOnItemClickListener((parent, view, position, id) ->
                startActivity(MaterialQuoteDetailActivity.intent(
                        this,
                        quotes.get(position).id())));
    }

    @Override
    protected void onStart() {
        super.onStart();
        runtime = runtime();
        binding.quoteNew.setVisibility(
                runtime.map(MaterialQuoteFeatureRuntime::draftWriteAllowed).orElse(false)
                        ? View.VISIBLE
                        : View.GONE);
        loadFirstPage();
    }

    @Override
    protected void onStop() {
        generation++;
        loading = false;
        runtime = Optional.empty();
        super.onStop();
    }

    private void loadFirstPage() {
        loadPage(true);
    }

    private void loadNextPage() {
        if (nextCursor.isPresent()) {
            loadPage(false);
        }
    }

    private void loadPage(boolean replace) {
        if (runtime.isEmpty() || loading) {
            binding.quoteNotice.setText(R.string.quote_failure);
            return;
        }
        loading = true;
        generation++;
        long operation = generation;
        binding.quoteProgress.setVisibility(View.VISIBLE);
        binding.quoteNotice.setText("");
        binding.quoteSearchAction.setEnabled(false);
        binding.quoteLoadMore.setEnabled(false);
        String search = binding.quoteSearch.getText() == null
                ? ""
                : binding.quoteSearch.getText().toString();
        Optional<String> cursor = replace ? Optional.empty() : nextCursor;
        MaterialQuoteFeatureRuntime available = runtime.orElseThrow();
        available.workerExecutor().execute(() -> {
            try {
                MaterialQuotePage page = available.quoteRepository().page(
                        new MaterialQuoteQuery(
                                Optional.of(search),
                                Optional.empty(),
                                view,
                                MaterialQuoteQuery.initial().pageSize()),
                        cursor);
                runOnUiThread(() -> showPage(operation, page, replace));
            } catch (MaterialQuoteException exception) {
                runOnUiThread(() -> showFailure(operation));
            }
        });
    }

    private void showPage(
            long operation,
            MaterialQuotePage page,
            boolean replace) {
        if (operation != generation) {
            return;
        }
        loading = false;
        binding.quoteProgress.setVisibility(View.INVISIBLE);
        binding.quoteSearchAction.setEnabled(true);
        if (replace) {
            quotes.clear();
        }
        quotes.addAll(page.items());
        nextCursor = page.nextCursor();
        adapter.clear();
        NumberFormat currency = NumberFormat.getCurrencyInstance(
                new Locale("pt", "BR"));
        quotes.forEach(quote -> adapter.add(
                quote.title()
                        + "\n"
                        + quote.customer().name()
                        + " • "
                        + currency.format(quote.total())
                        + " • "
                        + quote.status().name()));
        binding.quoteLoadMore.setVisibility(
                nextCursor.isPresent() ? View.VISIBLE : View.GONE);
        binding.quoteLoadMore.setEnabled(nextCursor.isPresent());
        if (quotes.isEmpty()) {
            binding.quoteNotice.setText(switch (view) {
                case ACTIVE -> R.string.quote_active_empty;
                case HISTORY -> R.string.quote_history_empty;
                case ALL -> R.string.quote_empty;
            });
        }
    }

    private void showFailure(long operation) {
        if (operation == generation) {
            loading = false;
            binding.quoteProgress.setVisibility(View.INVISIBLE);
            binding.quoteSearchAction.setEnabled(true);
            binding.quoteLoadMore.setEnabled(nextCursor.isPresent());
            binding.quoteNotice.setText(R.string.quote_failure);
        }
    }

    private Optional<MaterialQuoteFeatureRuntime> runtime() {
        if (getApplication() instanceof MaterialQuoteRuntimeProvider provider) {
            return provider.materialQuoteRuntime();
        }
        return Optional.empty();
    }
}
