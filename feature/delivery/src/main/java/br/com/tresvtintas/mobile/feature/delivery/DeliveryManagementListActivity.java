package br.com.tresvtintas.mobile.feature.delivery;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import br.com.tresvtintas.mobile.core.delivery.DeliveryException;
import br.com.tresvtintas.mobile.core.delivery.DeliveryFailureKind;
import br.com.tresvtintas.mobile.core.delivery.DeliveryManagementListController;
import br.com.tresvtintas.mobile.core.delivery.DeliveryManagementListState;
import br.com.tresvtintas.mobile.core.delivery.DeliveryManagementOrganization;
import br.com.tresvtintas.mobile.core.delivery.DeliveryManagementView;
import br.com.tresvtintas.mobile.feature.delivery.databinding.DeliveryManagementActivityListBinding;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class DeliveryManagementListActivity
        extends AppCompatActivity {
    private static final long SEARCH_DELAY_MILLIS = 350;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final DeliveryManagementListController.Listener listener =
            this::render;
    private DeliveryManagementActivityListBinding binding;
    private DeliveryManagementListRenderer renderer;
    private Optional<DeliveryManagementListController> controller =
            Optional.empty();
    private Optional<Runnable> pendingSearch = Optional.empty();
    private List<DeliveryManagementOrganization> organizations = List.of();

    public static Intent intent(Context context) {
        return new Intent(context, DeliveryManagementListActivity.class);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        DeliveryPrivacy.protect(this);
        EdgeToEdge.enable(this);
        binding = DeliveryManagementActivityListBinding.inflate(
                getLayoutInflater());
        DeliveryManagementSummaryAdapter adapter =
                new DeliveryManagementSummaryAdapter(this::openDetail);
        renderer = new DeliveryManagementListRenderer(binding, adapter);
        setContentView(binding.getRoot());
        DeliveryInsets.applySystemBars(binding.getRoot());
        binding.deliveryManagementList.setLayoutManager(
                new LinearLayoutManager(this));
        binding.deliveryManagementList.setAdapter(adapter);
        binding.deliveryManagementToolbar.setNavigationOnClickListener(
                ignored -> finish());
        binding.deliveryManagementRefresh.setOnClickListener(
                ignored -> controller.ifPresent(
                        DeliveryManagementListController::refresh));
        binding.deliveryManagementRetry.setOnClickListener(
                ignored -> controller.ifPresent(
                        DeliveryManagementListController::open));
        binding.deliveryManagementLoadMore.setOnClickListener(
                ignored -> controller.ifPresent(
                        DeliveryManagementListController::loadMore));
        binding.deliveryManagementViews.setOnCheckedStateChangeListener(
                (group, checked) -> {
                    if (!checked.isEmpty()) {
                        changeView(checked.get(0));
                    }
                });
        binding.deliveryManagementOrganization.setOnItemClickListener(
                (parent, view, position, id) -> {
                    if (position >= 0
                            && position < organizations.size()) {
                        controller.ifPresent(value ->
                                value.selectOrganization(
                                        organizations.get(position).id()));
                    }
                });
        binding.deliveryManagementSearch.addTextChangedListener(
                searchWatcher());
    }

    @Override
    protected void onStart() {
        super.onStart();
        Optional<DeliveryFeatureRuntime> runtime = runtime();
        if (runtime.isEmpty()
                || !runtime.orElseThrow().canManage()
                || runtime.orElseThrow().managementRepository().isEmpty()) {
            render(DeliveryManagementListState.error(
                    new DeliveryException(
                            DeliveryFailureKind.ACCESS_REVOKED,
                            "Delivery management runtime is unavailable.")));
            return;
        }
        DeliveryFeatureRuntime value = runtime.orElseThrow();
        DeliveryManagementListController next =
                new DeliveryManagementListController(
                        value.managementRepository().orElseThrow(),
                        value.workerExecutor(),
                        ContextCompat.getMainExecutor(this));
        controller = Optional.of(next);
        next.subscribe(listener);
        next.open();
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

    private void openDetail(long organizationId, long orderId) {
        startActivity(DeliveryManagementDetailActivity.intent(
                this,
                organizationId,
                orderId));
    }

    private void changeView(int id) {
        DeliveryManagementView view =
                id == R.id.delivery_management_view_awaiting
                        ? DeliveryManagementView.AWAITING_SCHEDULE
                        : id == R.id.delivery_management_view_scheduled
                                ? DeliveryManagementView.SCHEDULED
                                : id == R.id.delivery_management_view_all
                                        ? DeliveryManagementView.ALL
                                        : DeliveryManagementView.TODAY;
        controller.ifPresent(value -> value.currentQuery().ifPresent(query ->
                value.openQuery(query.withView(view))));
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
                item.currentQuery().ifPresent(query ->
                        item.openQuery(query.withSearch(value))));
        pendingSearch = Optional.of(action);
        handler.postDelayed(action, SEARCH_DELAY_MILLIS);
    }

    private void cancelSearch() {
        pendingSearch.ifPresent(handler::removeCallbacks);
        pendingSearch = Optional.empty();
    }

    private void render(DeliveryManagementListState state) {
        renderer.render(state);
        state.snapshot().ifPresent(this::organizations);
    }

    private void organizations(
            DeliveryManagementListState.Snapshot snapshot) {
        organizations = snapshot.organizations();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                organizations.stream()
                        .map(DeliveryManagementOrganization::name)
                        .collect(Collectors.toList()));
        binding.deliveryManagementOrganization.setAdapter(adapter);
        binding.deliveryManagementOrganization.setText(
                snapshot.selectedOrganization().name(),
                false);
    }

    private Optional<DeliveryFeatureRuntime> runtime() {
        return getApplication()
                        instanceof DeliveryRuntimeProvider provider
                ? provider.deliveryRuntime()
                : Optional.empty();
    }
}
