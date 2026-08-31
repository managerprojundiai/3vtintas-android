package br.com.tresvtintas.mobile.feature.catalog;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import br.com.tresvtintas.mobile.core.catalog.CatalogCategory;
import br.com.tresvtintas.mobile.core.catalog.CatalogController;
import br.com.tresvtintas.mobile.core.catalog.CatalogException;
import br.com.tresvtintas.mobile.core.catalog.CatalogFailureKind;
import br.com.tresvtintas.mobile.core.catalog.CatalogProduct;
import br.com.tresvtintas.mobile.core.catalog.CatalogPricingContext;
import br.com.tresvtintas.mobile.core.catalog.CatalogPricingContextListener;
import br.com.tresvtintas.mobile.core.catalog.CatalogQuery;
import br.com.tresvtintas.mobile.core.catalog.CatalogState;
import br.com.tresvtintas.mobile.core.catalog.CatalogStateListener;
import br.com.tresvtintas.mobile.feature.catalog.databinding.CatalogActivityBinding;
import br.com.tresvtintas.mobile.feature.catalogadmin.CatalogAdministrationActivity;
import br.com.tresvtintas.mobile.feature.catalogadmin.CatalogAdministrationRuntimeProvider;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.stream.Collectors;

public final class CatalogActivity extends AppCompatActivity {
    private static final long SEARCH_DELAY_MILLIS = 450;
    private static final int MINIMUM_SEARCH_LENGTH = 2;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private final Map<Long, CatalogCategory> knownCategories = new LinkedHashMap<>();
    private final List<CatalogCategoryOption> categoryOptions = new ArrayList<>();
    private final CatalogStateListener stateListener = this::render;
    private final CatalogPricingContextListener pricingListener = this::renderPricing;
    private final List<CatalogPriceListOptionView> priceListOptions = new ArrayList<>();
    private CatalogActivityBinding binding;
    private CatalogScreenRenderer renderer;
    private Optional<CatalogController> controller = Optional.empty();
    private Optional<Runnable> pendingSearch = Optional.empty();
    private boolean changingCategoryOptions;
    private boolean changingPriceListOptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = CatalogActivityBinding.inflate(getLayoutInflater());
        CatalogProductAdapter adapter = new CatalogProductAdapter();
        renderer = new CatalogScreenRenderer(binding, adapter);
        setContentView(binding.getRoot());
        applySystemBarInsets(binding.getRoot());
        binding.catalogList.setLayoutManager(new LinearLayoutManager(this));
        binding.catalogList.setAdapter(adapter);
        binding.catalogToolbar.setNavigationOnClickListener(ignored -> finish());
        binding.catalogRefresh.setOnClickListener(ignored -> refresh());
        binding.catalogRetry.setOnClickListener(ignored -> retry());
        binding.catalogLoadMore.setOnClickListener(ignored -> loadMore());
        binding.catalogCategory.setOnItemClickListener(
                (parent, view, position, id) -> selectCategory(position));
        binding.catalogPriceList.setOnItemClickListener(
                (parent, view, position, id) -> selectPriceList(position));
        binding.catalogSearch.addTextChangedListener(searchWatcher());
        binding.catalogSearch.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_SEARCH) {
                return false;
            }
            runSearch(text(binding.catalogSearch));
            return true;
        });
        updateCategoryOptions(OptionalLong.empty());
    }

    @Override
    protected void onStart() {
        super.onStart();
        configureAdministrationAccess();
        Optional<CatalogController> available = provider()
                .flatMap(CatalogRuntimeProvider::catalogController);
        if (available.isEmpty()) {
            render(accessRevokedState());
            return;
        }
        CatalogController next = available.orElseThrow();
        if (controller.filter(next::equals).isEmpty()) {
            detachController();
            controller = Optional.of(next);
            next.subscribe(stateListener);
            next.subscribePricing(pricingListener);
            if (next.currentState().phase() == CatalogState.Phase.EMPTY) {
                next.open(CatalogQuery.initial());
            }
        }
    }

    @Override
    protected void onStop() {
        detachController();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        cancelPendingSearch();
        super.onDestroy();
    }

    private void render(CatalogState state) {
        CatalogUiState uiState = CatalogUiState.from(state);
        renderer.render(uiState);
        state.snapshot().ifPresent(snapshot -> {
            rememberCategories(snapshot.items());
            updateCategoryOptions(controller
                    .map(CatalogController::currentQuery)
                    .map(CatalogQuery::categoryId)
                    .orElseGet(OptionalLong::empty));
        });
    }

    private void renderPricing(Optional<CatalogPricingContext> context) {
        binding.catalogPriceListContainer.setVisibility(
                context.isPresent() ? View.VISIBLE : View.GONE);
        if (context.isEmpty()) {
            return;
        }
        CatalogPricingContext value = context.orElseThrow();
        changingPriceListOptions = true;
        priceListOptions.clear();
        value.options().stream()
                .map(CatalogPriceListOptionView::from)
                .forEach(priceListOptions::add);
        binding.catalogPriceList.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                priceListOptions.stream()
                        .map(CatalogPriceListOptionView::label)
                        .collect(Collectors.toList())));
        value.selectedVersionPublicId().ifPresentOrElse(
                selected -> priceListOptions.stream()
                        .filter(option -> option.versionPublicId().equals(selected))
                        .findFirst()
                        .ifPresent(option -> binding.catalogPriceList.setText(
                                option.label(), false)),
                () -> binding.catalogPriceList.setText("", false));
        binding.catalogPriceList.setEnabled(
                value.selectionRequired() || value.options().size() > 1);
        changingPriceListOptions = false;
    }

    private void refresh() {
        controller.ifPresent(CatalogController::refresh);
    }

    private void retry() {
        controller.ifPresent(value -> value.open(value.currentQuery()));
    }

    private void loadMore() {
        controller.ifPresent(CatalogController::loadMore);
    }

    private TextWatcher searchWatcher() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence value,
                    int start,
                    int count,
                    int after) {
                // No pre-change action.
            }

            @Override
            public void onTextChanged(
                    CharSequence value,
                    int start,
                    int before,
                    int count) {
                scheduleSearch(value == null ? "" : value.toString());
            }

            @Override
            public void afterTextChanged(Editable value) {
                // The debounced query is scheduled in onTextChanged.
            }
        };
    }

    private void scheduleSearch(String search) {
        cancelPendingSearch();
        String normalizedSearch = search.trim();
        if (!normalizedSearch.isEmpty()
                && normalizedSearch.length() < MINIMUM_SEARCH_LENGTH) {
            binding.catalogSearchContainer.setHelperText(
                    getString(R.string.catalog_search_minimum));
            return;
        }
        binding.catalogSearchContainer.setHelperText(
                getString(R.string.catalog_search_example));
        Runnable searchAction = () -> runSearch(normalizedSearch);
        pendingSearch = Optional.of(searchAction);
        searchHandler.postDelayed(searchAction, SEARCH_DELAY_MILLIS);
    }

    private void runSearch(String search) {
        cancelPendingSearch();
        String normalizedSearch = search.trim();
        if (!normalizedSearch.isEmpty()
                && normalizedSearch.length() < MINIMUM_SEARCH_LENGTH) {
            binding.catalogSearchContainer.setHelperText(
                    getString(R.string.catalog_search_minimum));
            return;
        }
        binding.catalogSearchContainer.setHelperText(
                getString(R.string.catalog_search_example));
        controller.ifPresent(value -> value.open(
                value.currentQuery().withSearch(normalizedSearch)));
    }

    private void cancelPendingSearch() {
        pendingSearch.ifPresent(searchHandler::removeCallbacks);
        pendingSearch = Optional.empty();
    }

    private void selectCategory(int position) {
        if (changingCategoryOptions
                || controller.isEmpty()
                || position < 0
                || position >= categoryOptions.size()) {
            return;
        }
        controller.ifPresent(value -> value.open(
                value.currentQuery().withCategory(categoryOptions.get(position).id())));
    }

    private void selectPriceList(int position) {
        if (changingPriceListOptions
                || controller.isEmpty()
                || position < 0
                || position >= priceListOptions.size()) {
            return;
        }
        controller.orElseThrow().selectPriceList(
                priceListOptions.get(position).versionPublicId());
    }

    private void rememberCategories(List<CatalogProduct> products) {
        for (CatalogProduct product : products) {
            product.category().ifPresent(category ->
                    knownCategories.put(category.id(), category));
        }
    }

    private void updateCategoryOptions(OptionalLong selectedId) {
        changingCategoryOptions = true;
        categoryOptions.clear();
        categoryOptions.add(CatalogCategoryOption.all(
                getString(R.string.catalog_all_categories)));
        knownCategories.values().stream()
                .sorted(Comparator.comparing(
                        CatalogCategory::name,
                        String.CASE_INSENSITIVE_ORDER))
                .map(CatalogCategoryOption::from)
                .forEach(categoryOptions::add);
        List<String> labels = categoryOptions.stream()
                .map(CatalogCategoryOption::label)
                .collect(Collectors.toList());
        binding.catalogCategory.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                labels));
        int selectedIndex = 0;
        for (int index = 0; index < categoryOptions.size(); index++) {
            if (categoryOptions.get(index).id().equals(selectedId)) {
                selectedIndex = index;
                break;
            }
        }
        binding.catalogCategory.setText(
                categoryOptions.get(selectedIndex).label(),
                false);
        changingCategoryOptions = false;
    }

    private Optional<CatalogRuntimeProvider> provider() {
        if (getApplication() instanceof CatalogRuntimeProvider runtimeProvider) {
            return Optional.of(runtimeProvider);
        }
        return Optional.empty();
    }

    private void configureAdministrationAccess() {
        boolean available = getApplication()
                instanceof CatalogAdministrationRuntimeProvider provider
                && provider.catalogAdministrationRuntime().isPresent();
        binding.catalogManage.setVisibility(
                available ? View.VISIBLE : View.GONE);
        binding.catalogManage.setOnClickListener(available
                ? ignored -> startActivity(
                        CatalogAdministrationActivity.intent(this))
                : null);
    }

    private void detachController() {
        controller.ifPresent(value -> {
            value.unsubscribe(stateListener);
            value.unsubscribePricing(pricingListener);
        });
        controller = Optional.empty();
    }

    private static CatalogState accessRevokedState() {
        return CatalogState.error(new CatalogException(
                CatalogFailureKind.ACCESS_REVOKED,
                "Catalog access is not active."));
    }

    private static String text(android.widget.TextView field) {
        return field.getText() == null ? "" : field.getText().toString();
    }

    private static void applySystemBarInsets(View root) {
        int left = root.getPaddingLeft();
        int top = root.getPaddingTop();
        int right = root.getPaddingRight();
        int bottom = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(
                    left + bars.left,
                    top + bars.top,
                    right + bars.right,
                    bottom + bars.bottom);
            return windowInsets;
        });
    }
}
