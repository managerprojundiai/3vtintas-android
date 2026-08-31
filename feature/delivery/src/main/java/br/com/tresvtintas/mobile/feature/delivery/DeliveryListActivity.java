package br.com.tresvtintas.mobile.feature.delivery;

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
import br.com.tresvtintas.mobile.core.delivery.DeliveryException;
import br.com.tresvtintas.mobile.core.delivery.DeliveryFailureKind;
import br.com.tresvtintas.mobile.core.delivery.DeliveryListController;
import br.com.tresvtintas.mobile.core.delivery.DeliveryListState;
import br.com.tresvtintas.mobile.core.delivery.DeliveryQuery;
import br.com.tresvtintas.mobile.core.delivery.DeliveryView;
import br.com.tresvtintas.mobile.feature.delivery.databinding.DeliveryActivityListBinding;
import java.util.Optional;

public final class DeliveryListActivity extends AppCompatActivity {
    private static final long SEARCH_DELAY_MILLIS = 350;
    private final Handler handler = new Handler(
            Looper.getMainLooper());
    private final DeliveryListController.Listener listener =
            this::render;
    private DeliveryActivityListBinding binding;
    private DeliveryListRenderer renderer;
    private Optional<DeliveryListController> controller =
            Optional.empty();
    private Optional<Runnable> pendingSearch = Optional.empty();

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        DeliveryPrivacy.protect(this);
        EdgeToEdge.enable(this);
        binding = DeliveryActivityListBinding.inflate(
                getLayoutInflater());
        DeliverySummaryAdapter adapter =
                new DeliverySummaryAdapter(this::openDetail);
        renderer = new DeliveryListRenderer(binding, adapter);
        setContentView(binding.getRoot());
        DeliveryInsets.applySystemBars(binding.getRoot());
        binding.deliveryList.setLayoutManager(
                new LinearLayoutManager(this));
        binding.deliveryList.setAdapter(adapter);
        binding.deliveryToolbar.setNavigationOnClickListener(
                ignored -> finish());
        binding.deliveryRefresh.setOnClickListener(
                ignored -> controller.ifPresent(
                        DeliveryListController::refresh));
        binding.deliveryManage.setOnClickListener(ignored ->
                startActivity(DeliveryManagementListActivity.intent(this)));
        binding.deliveryItinerary.setOnClickListener(ignored ->
                startActivity(DeliveryRouteActivity.intent(this)));
        binding.deliveryRetry.setOnClickListener(
                ignored -> retry());
        binding.deliveryLoadMore.setOnClickListener(
                ignored -> controller.ifPresent(
                        DeliveryListController::loadMore));
        binding.deliveryViews.setOnCheckedStateChangeListener(
                (group, checked) -> {
                    if (!checked.isEmpty()) {
                        changeView(checked.get(0));
                    }
                });
        binding.deliverySearch.addTextChangedListener(
                searchWatcher());
    }

    @Override
    protected void onStart() {
        super.onStart();
        Optional<DeliveryFeatureRuntime> runtime = runtime();
        if (runtime.isEmpty()) {
            render(DeliveryListState.error(new DeliveryException(
                    DeliveryFailureKind.ACCESS_REVOKED,
                    "Delivery runtime is unavailable.")));
            return;
        }
        DeliveryFeatureRuntime value = runtime.orElseThrow();
        binding.deliveryManage.setVisibility(
                value.canManage() ? View.VISIBLE : View.GONE);
        binding.deliveryItinerary.setVisibility(
                value.hasOwnItinerary() ? View.VISIBLE : View.GONE);
        DeliveryListController next = new DeliveryListController(
                value.repository(),
                value.workerExecutor(),
                ContextCompat.getMainExecutor(this));
        controller = Optional.of(next);
        next.subscribe(listener);
        next.open(DeliveryQuery.initial().withSearch(
                text(binding.deliverySearch.getText())));
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

    private void openDetail(long id) {
        startActivity(DeliveryDetailActivity.intent(this, id));
    }

    private void retry() {
        controller.ifPresent(value -> value.open(
                value.currentQuery()));
    }

    private void changeView(int id) {
        DeliveryView view = id == R.id.delivery_view_history
                ? DeliveryView.HISTORY
                : id == R.id.delivery_view_all
                        ? DeliveryView.ALL
                        : DeliveryView.ACTIVE;
        controller.ifPresent(value -> value.open(
                value.currentQuery().withView(view)));
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
                scheduleSearch(value == null
                        ? ""
                        : value.toString());
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

    private void render(DeliveryListState state) {
        renderer.render(state);
    }

    private Optional<DeliveryFeatureRuntime> runtime() {
        return getApplication() instanceof DeliveryRuntimeProvider provider
                ? provider.deliveryRuntime()
                : Optional.empty();
    }

    private static String text(Editable value) {
        return value == null ? "" : value.toString();
    }
}
