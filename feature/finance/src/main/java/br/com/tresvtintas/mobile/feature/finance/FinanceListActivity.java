package br.com.tresvtintas.mobile.feature.finance;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import br.com.tresvtintas.mobile.core.finance.FinanceDueFilter;
import br.com.tresvtintas.mobile.core.finance.FinanceEntryStatus;
import br.com.tresvtintas.mobile.core.finance.FinanceEntrySource;
import br.com.tresvtintas.mobile.core.finance.FinanceEntryType;
import br.com.tresvtintas.mobile.core.finance.FinanceException;
import br.com.tresvtintas.mobile.core.finance.FinanceFailureKind;
import br.com.tresvtintas.mobile.core.finance.FinanceListController;
import br.com.tresvtintas.mobile.core.finance.FinanceListState;
import br.com.tresvtintas.mobile.core.finance.FinanceListStateListener;
import br.com.tresvtintas.mobile.core.finance.FinanceQuery;
import br.com.tresvtintas.mobile.feature.finance.databinding.FinanceActivityListBinding;
import java.util.Optional;

public final class FinanceListActivity extends AppCompatActivity {
    private static final long SEARCH_DELAY_MILLIS = 350;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final FinanceListStateListener listener = this::render;
    private FinanceActivityListBinding binding;
    private FinanceListRenderer renderer;
    private Optional<FinanceListController> controller = Optional.empty();
    private Optional<Runnable> pendingSearch = Optional.empty();
    private FinanceRoute route = FinanceRoute.personal();

    public static Intent intent(Context context) {
        return intent(context, FinanceRoute.personal());
    }

    public static Intent intent(Context context, FinanceRoute route) {
        return route.apply(new Intent(context, FinanceListActivity.class));
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        FinancePrivacy.protect(this);
        EdgeToEdge.enable(this);
        route = FinanceRoute.from(getIntent());
        binding = FinanceActivityListBinding.inflate(getLayoutInflater());
        FinanceSummaryAdapter adapter =
                new FinanceSummaryAdapter(this::openDetail);
        renderer = new FinanceListRenderer(binding, adapter);
        setContentView(binding.getRoot());
        FinanceInsets.applySystemBars(binding.getRoot());
        binding.financeToolbar.setTitle(route.experience()
                == br.com.tresvtintas.mobile.core.finance.FinanceExperience.CORPORATE
                        ? R.string.finance_corporate_title
                        : R.string.finance_title);
        binding.financeToolbar.setSubtitle(subtitle());
        binding.financeCreate.setVisibility(
                route.canCreate() ? View.VISIBLE : View.GONE);
        binding.financeSourceFilterContainer.setVisibility(
                route.experience()
                                == br.com.tresvtintas.mobile.core.finance.FinanceExperience.CORPORATE
                        ? View.VISIBLE
                        : View.GONE);
        binding.financeList.setLayoutManager(new LinearLayoutManager(this));
        binding.financeList.setAdapter(adapter);
        binding.financeToolbar.setNavigationOnClickListener(
                ignored -> finish());
        binding.financeRefresh.setOnClickListener(
                ignored -> controller.ifPresent(
                        FinanceListController::refresh));
        binding.financeRetry.setOnClickListener(ignored -> retry());
        binding.financeLoadMore.setOnClickListener(
                ignored -> controller.ifPresent(
                        FinanceListController::loadMore));
        binding.financeCreate.setOnClickListener(
                ignored -> startActivity(
                        FinanceCreateActivity.intent(this, route)));
        binding.financeStatusFilters.setOnCheckedStateChangeListener(
                (group, checked) -> {
                    if (!checked.isEmpty()) {
                        changeStatus(checked.get(0));
                    }
                });
        binding.financeTypeFilters.setOnCheckedStateChangeListener(
                (group, checked) -> {
                    if (!checked.isEmpty()) {
                        changeType(checked.get(0));
                    }
                });
        binding.financeSourceFilters.setOnCheckedStateChangeListener(
                (group, checked) -> {
                    if (!checked.isEmpty()) {
                        changeSource(checked.get(0));
                    }
                });
        binding.financeDueFilters.setOnCheckedStateChangeListener(
                (group, checked) -> {
                    if (!checked.isEmpty()) {
                        changeDue(checked.get(0));
                    }
                });
        binding.financeSearch.addTextChangedListener(searchWatcher());
    }

    @Override
    protected void onStart() {
        super.onStart();
        Optional<FinanceFeatureRuntime> runtime = runtime();
        if (runtime.isEmpty()) {
            render(FinanceListState.error(new FinanceException(
                    FinanceFailureKind.ACCESS_REVOKED,
                    "Finance runtime is unavailable.")));
            return;
        }
        FinanceFeatureRuntime value = runtime.orElseThrow();
        FinanceQuery initial = FinanceQuery.initial()
                .withSearch(text(binding.financeSearch.getText()))
                .withStatus(statusFor(
                        binding.financeStatusFilters.getCheckedChipId()))
                .withType(typeFor(
                        binding.financeTypeFilters.getCheckedChipId()))
                .withSource(sourceFor(
                        binding.financeSourceFilters.getCheckedChipId()))
                .withDue(dueFor(
                        binding.financeDueFilters.getCheckedChipId()));
        FinanceListController next = new FinanceListController(
                value.repository(),
                initial,
                value.workerExecutor(),
                ContextCompat.getMainExecutor(this));
        controller = Optional.of(next);
        next.subscribe(listener);
        next.open(initial);
    }

    @Override
    protected void onStop() {
        controller.ifPresent(value -> {
            value.unsubscribe(listener);
            value.close();
        });
        controller = Optional.empty();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        cancelSearch();
        super.onDestroy();
    }

    private void openDetail(long entryId) {
        startActivity(FinanceDetailActivity.intent(this, entryId, route));
    }

    private void retry() {
        controller.ifPresent(value -> value.open(value.currentQuery()));
    }

    private void changeStatus(int id) {
        controller.ifPresent(value ->
                value.open(value.currentQuery().withStatus(statusFor(id))));
    }

    private void changeType(int id) {
        controller.ifPresent(value ->
                value.open(value.currentQuery().withType(typeFor(id))));
    }

    private void changeSource(int id) {
        controller.ifPresent(value ->
                value.open(value.currentQuery().withSource(sourceFor(id))));
    }

    private void changeDue(int id) {
        controller.ifPresent(value ->
                value.open(value.currentQuery().withDue(dueFor(id))));
    }

    private static FinanceEntryStatus statusFor(int id) {
        return id == R.id.finance_status_pending
                ? FinanceEntryStatus.PENDING
                : id == R.id.finance_status_settled
                        ? FinanceEntryStatus.SETTLED
                        : id == R.id.finance_status_cancelled
                                ? FinanceEntryStatus.CANCELLED
                                : null;
    }

    private static FinanceEntryType typeFor(int id) {
        return id == R.id.finance_type_expense
                ? FinanceEntryType.EXPENSE
                : id == R.id.finance_type_payable
                        ? FinanceEntryType.PAYABLE
                        : id == R.id.finance_type_receivable
                                ? FinanceEntryType.RECEIVABLE
                                : null;
    }

    private static FinanceEntrySource sourceFor(int id) {
        return id == R.id.finance_source_manual
                ? FinanceEntrySource.MANUAL
                : id == R.id.finance_source_material_order
                        ? FinanceEntrySource.MATERIAL_ORDER
                        : id == R.id.finance_source_commission
                                ? FinanceEntrySource.COMMISSION
                                : id == R.id.finance_source_system
                                        ? FinanceEntrySource.SYSTEM
                                        : null;
    }

    private static FinanceDueFilter dueFor(int id) {
        return id == R.id.finance_due_overdue
                ? FinanceDueFilter.OVERDUE
                : id == R.id.finance_due_upcoming
                        ? FinanceDueFilter.UPCOMING
                        : id == R.id.finance_due_without_date
                                ? FinanceDueFilter.WITHOUT_DUE_DATE
                                : FinanceDueFilter.ALL;
    }

    private TextWatcher searchWatcher() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence value,
                    int start,
                    int count,
                    int after) {
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
            }
        };
    }

    private void scheduleSearch(String value) {
        cancelSearch();
        Runnable action = () -> controller.ifPresent(item ->
                item.open(item.currentQuery().withSearch(value)));
        pendingSearch = Optional.of(action);
        handler.postDelayed(action, SEARCH_DELAY_MILLIS);
    }

    private void cancelSearch() {
        pendingSearch.ifPresent(handler::removeCallbacks);
        pendingSearch = Optional.empty();
    }

    private void render(FinanceListState state) {
        renderer.render(state);
    }

    private Optional<FinanceFeatureRuntime> runtime() {
        return getApplication() instanceof FinanceRuntimeProvider provider
                ? provider.financeRuntime(route)
                : Optional.empty();
    }

    private String subtitle() {
        if (route.experience()
                == br.com.tresvtintas.mobile.core.finance.FinanceExperience.PERSONAL) {
            return getString(R.string.finance_subtitle);
        }
        return route.organizationName()
                .map(value -> getString(
                        R.string.finance_corporate_organization_subtitle,
                        value))
                .orElseGet(() -> getString(
                        R.string.finance_corporate_global_subtitle));
    }

    private static String text(Editable value) {
        return value == null ? "" : value.toString();
    }
}
