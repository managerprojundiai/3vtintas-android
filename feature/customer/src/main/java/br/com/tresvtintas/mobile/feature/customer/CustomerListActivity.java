package br.com.tresvtintas.mobile.feature.customer;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import br.com.tresvtintas.mobile.core.customer.CustomerException;
import br.com.tresvtintas.mobile.core.customer.CustomerFailureKind;
import br.com.tresvtintas.mobile.core.customer.CustomerListController;
import br.com.tresvtintas.mobile.core.customer.CustomerListState;
import br.com.tresvtintas.mobile.core.customer.CustomerListStateListener;
import br.com.tresvtintas.mobile.core.customer.CustomerQuery;
import br.com.tresvtintas.mobile.feature.customer.databinding.CustomerActivityListBinding;
import java.util.Optional;

public final class CustomerListActivity extends AppCompatActivity {
    private static final long SEARCH_DELAY_MILLIS = 350;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private final CustomerListStateListener stateListener = this::render;
    private CustomerActivityListBinding binding;
    private CustomerListRenderer renderer;
    private Optional<CustomerListController> controller = Optional.empty();
    private Optional<Runnable> pendingSearch = Optional.empty();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CustomerPrivacy.protect(this);
        EdgeToEdge.enable(this);
        binding = CustomerActivityListBinding.inflate(getLayoutInflater());
        CustomerSummaryAdapter adapter = new CustomerSummaryAdapter(
                this::openCustomer);
        renderer = new CustomerListRenderer(binding, adapter);
        setContentView(binding.getRoot());
        CustomerInsets.applySystemBars(binding.getRoot());
        binding.customerList.setLayoutManager(new LinearLayoutManager(this));
        binding.customerList.setAdapter(adapter);
        binding.customerToolbar.setNavigationOnClickListener(
                ignored -> finish());
        binding.customerRefresh.setOnClickListener(ignored -> refresh());
        binding.customerRetry.setOnClickListener(ignored -> retry());
        binding.customerLoadMore.setOnClickListener(ignored -> loadMore());
        binding.customerAdd.setOnClickListener(ignored -> openCreate());
        binding.customerSearch.addTextChangedListener(searchWatcher());
    }

    @Override
    protected void onStart() {
        super.onStart();
        Optional<CustomerFeatureRuntime> runtime = runtime();
        if (runtime.isEmpty()) {
            render(CustomerListState.error(new CustomerException(
                    CustomerFailureKind.ACCESS_REVOKED,
                    "Customer runtime is unavailable.")));
            return;
        }
        CustomerFeatureRuntime available = runtime.orElseThrow();
        renderer.writeAllowed(available.writeAllowed());
        CustomerListController next = new CustomerListController(
                available.repository(),
                available.workerExecutor(),
                ContextCompat.getMainExecutor(this));
        controller = Optional.of(next);
        next.subscribe(stateListener);
        next.open(CustomerQuery.initial().withSearch(
                text(binding.customerSearch.getText())));
    }

    @Override
    protected void onStop() {
        controller.ifPresent(value -> {
            value.unsubscribe(stateListener);
            value.close();
        });
        controller = Optional.empty();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        cancelPendingSearch();
        super.onDestroy();
    }

    private void render(CustomerListState state) {
        renderer.render(state);
    }

    private void refresh() {
        controller.ifPresent(CustomerListController::refresh);
    }

    private void retry() {
        controller.ifPresent(value -> value.open(value.currentQuery()));
    }

    private void loadMore() {
        controller.ifPresent(CustomerListController::loadMore);
    }

    private void openCustomer(long customerId) {
        startActivity(CustomerDetailActivity.intent(this, customerId));
    }

    private void openCreate() {
        startActivity(new Intent(this, CustomerEditActivity.class));
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
        Runnable action = () -> controller.ifPresent(value ->
                value.open(value.currentQuery().withSearch(search)));
        pendingSearch = Optional.of(action);
        searchHandler.postDelayed(action, SEARCH_DELAY_MILLIS);
    }

    private void cancelPendingSearch() {
        pendingSearch.ifPresent(searchHandler::removeCallbacks);
        pendingSearch = Optional.empty();
    }

    private Optional<CustomerFeatureRuntime> runtime() {
        if (getApplication() instanceof CustomerRuntimeProvider provider) {
            return provider.customerRuntime();
        }
        return Optional.empty();
    }

    private static String text(Editable value) {
        return value == null ? "" : value.toString();
    }
}
