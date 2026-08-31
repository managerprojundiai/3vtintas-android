package br.com.tresvtintas.mobile.feature.corporatefinance;

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
import br.com.tresvtintas.mobile.core.corporatefinance.CorporateFinanceOrganization;
import br.com.tresvtintas.mobile.core.corporatefinance.CorporateFinanceOrganizationController;
import br.com.tresvtintas.mobile.core.corporatefinance.CorporateFinanceOrganizationState;
import br.com.tresvtintas.mobile.core.corporatefinance.CorporateFinanceOrganizationStateListener;
import br.com.tresvtintas.mobile.core.finance.FinanceFailureKind;
import br.com.tresvtintas.mobile.feature.corporatefinance.databinding.CorporateFinanceActivityOrganizationsBinding;
import br.com.tresvtintas.mobile.feature.finance.FinanceListActivity;
import br.com.tresvtintas.mobile.feature.finance.FinancePrivacy;
import br.com.tresvtintas.mobile.feature.finance.FinanceRoute;
import java.util.List;
import java.util.Optional;

public final class CorporateFinanceOrganizationActivity
        extends AppCompatActivity {
    private static final long SEARCH_DELAY_MILLIS = 350L;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final CorporateFinanceOrganizationStateListener listener =
            this::render;
    private CorporateFinanceActivityOrganizationsBinding binding;
    private CorporateFinanceOrganizationAdapter adapter;
    private Optional<CorporateFinanceOrganizationController> controller =
            Optional.empty();
    private Optional<Runnable> pendingSearch = Optional.empty();
    private boolean globalAccess;

    public static Intent intent(Context context) {
        return new Intent(
                context,
                CorporateFinanceOrganizationActivity.class);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        FinancePrivacy.protect(this);
        EdgeToEdge.enable(this);
        binding = CorporateFinanceActivityOrganizationsBinding.inflate(
                getLayoutInflater());
        adapter = new CorporateFinanceOrganizationAdapter(
                this::openOrganization);
        setContentView(binding.getRoot());
        applyInsets();
        binding.corporateFinanceOrganizations.setLayoutManager(
                new LinearLayoutManager(this));
        binding.corporateFinanceOrganizations.setAdapter(adapter);
        binding.corporateFinanceToolbar.setNavigationOnClickListener(
                ignored -> finish());
        binding.corporateFinanceGlobal.setOnClickListener(
                ignored -> startActivity(FinanceListActivity.intent(
                        this,
                        FinanceRoute.corporateGlobal())));
        binding.corporateFinanceRetry.setOnClickListener(
                ignored -> openCurrentSearch());
        binding.corporateFinanceLoadMore.setOnClickListener(
                ignored -> controller.ifPresent(
                        CorporateFinanceOrganizationController::loadMore));
        binding.corporateFinanceSearch.addTextChangedListener(
                searchWatcher());
    }

    @Override
    protected void onStart() {
        super.onStart();
        Optional<CorporateFinanceFeatureRuntime> runtime = runtime();
        if (runtime.isEmpty()) {
            render(CorporateFinanceOrganizationState.error(
                    new br.com.tresvtintas.mobile.core.finance.FinanceException(
                            FinanceFailureKind.ACCESS_REVOKED,
                            "Corporate finance runtime is unavailable.")));
            return;
        }
        CorporateFinanceFeatureRuntime value = runtime.orElseThrow();
        globalAccess = value.globalAccess();
        binding.corporateFinanceGlobal.setVisibility(
                globalAccess ? View.VISIBLE : View.GONE);
        CorporateFinanceOrganizationController next =
                new CorporateFinanceOrganizationController(
                        value.organizationRepository(),
                        value.workerExecutor(),
                        ContextCompat.getMainExecutor(this));
        controller = Optional.of(next);
        next.subscribe(listener);
        openCurrentSearch();
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

    private void openOrganization(
            CorporateFinanceOrganization organization) {
        startActivity(FinanceListActivity.intent(
                this,
                FinanceRoute.corporate(
                        organization.id(),
                        organization.name())));
    }

    private void openCurrentSearch() {
        controller.ifPresent(value -> value.open(text(
                binding.corporateFinanceSearch.getText())));
    }

    private void render(CorporateFinanceOrganizationState state) {
        boolean busy =
                state.phase()
                                == CorporateFinanceOrganizationState.Phase.LOADING
                        || state.phase()
                                == CorporateFinanceOrganizationState.Phase.LOADING_MORE;
        binding.corporateFinanceProgress.setVisibility(
                busy ? View.VISIBLE : View.INVISIBLE);
        binding.corporateFinanceRetry.setVisibility(View.GONE);
        binding.corporateFinanceNotice.setVisibility(View.GONE);
        if (state.phase()
                == CorporateFinanceOrganizationState.Phase.ERROR) {
            adapter.submitList(List.of());
            binding.corporateFinanceOrganizations.setVisibility(View.GONE);
            binding.corporateFinanceLoadMore.setVisibility(View.GONE);
            binding.corporateFinanceEmpty.setVisibility(View.VISIBLE);
            binding.corporateFinanceEmptyTitle.setText(
                    R.string.corporate_finance_error_title);
            binding.corporateFinanceEmptyMessage.setText(
                    failure(state.failure().orElseThrow()));
            binding.corporateFinanceRetry.setVisibility(View.VISIBLE);
            state.requestId().ifPresent(this::supportCode);
            return;
        }
        state.snapshot().ifPresentOrElse(snapshot -> {
            adapter.submitList(snapshot.items());
            boolean empty = snapshot.items().isEmpty();
            binding.corporateFinanceOrganizations.setVisibility(
                    empty ? View.GONE : View.VISIBLE);
            binding.corporateFinanceEmpty.setVisibility(
                    empty ? View.VISIBLE : View.GONE);
            binding.corporateFinanceEmptyTitle.setText(
                    R.string.corporate_finance_empty_title);
            binding.corporateFinanceEmptyMessage.setText(
                    R.string.corporate_finance_empty_message);
            binding.corporateFinanceLoadMore.setVisibility(
                    !empty && snapshot.hasMore()
                            ? View.VISIBLE
                            : View.GONE);
            binding.corporateFinanceLoadMore.setEnabled(!busy);
        }, () -> {
            binding.corporateFinanceOrganizations.setVisibility(View.GONE);
            binding.corporateFinanceLoadMore.setVisibility(View.GONE);
            binding.corporateFinanceEmpty.setVisibility(View.GONE);
        });
    }

    private int failure(FinanceFailureKind kind) {
        return switch (kind) {
            case AUTH_REJECTED, ACCESS_REVOKED ->
                R.string.corporate_finance_error_session;
            case FORBIDDEN ->
                R.string.corporate_finance_error_forbidden;
            case NETWORK ->
                R.string.corporate_finance_error_network;
            case RATE_LIMITED ->
                R.string.corporate_finance_error_rate;
            case UPDATE_REQUIRED ->
                R.string.corporate_finance_error_update;
            case SERVICE_UNAVAILABLE ->
                R.string.corporate_finance_error_service;
            default ->
                R.string.corporate_finance_error_generic;
        };
    }

    private void supportCode(String requestId) {
        binding.corporateFinanceNotice.setText(getString(
                R.string.corporate_finance_support_code,
                requestId));
        binding.corporateFinanceNotice.setVisibility(View.VISIBLE);
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
        Runnable action = () -> controller.ifPresent(
                item -> item.open(value));
        pendingSearch = Optional.of(action);
        handler.postDelayed(action, SEARCH_DELAY_MILLIS);
    }

    private void cancelSearch() {
        pendingSearch.ifPresent(handler::removeCallbacks);
        pendingSearch = Optional.empty();
    }

    private Optional<CorporateFinanceFeatureRuntime> runtime() {
        return getApplication()
                        instanceof CorporateFinanceRuntimeProvider provider
                ? provider.corporateFinanceRuntime()
                : Optional.empty();
    }

    private void applyInsets() {
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(
                binding.getRoot(),
                (view, insets) -> {
                    androidx.core.graphics.Insets bars = insets.getInsets(
                            androidx.core.view.WindowInsetsCompat.Type
                                    .systemBars());
                    view.setPadding(
                            bars.left,
                            bars.top,
                            bars.right,
                            bars.bottom);
                    return insets;
                });
    }

    private static String text(Editable value) {
        return value == null ? "" : value.toString();
    }
}
