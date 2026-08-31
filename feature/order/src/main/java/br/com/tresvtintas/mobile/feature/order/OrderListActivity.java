package br.com.tresvtintas.mobile.feature.order;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import br.com.tresvtintas.mobile.core.order.OrderException;
import br.com.tresvtintas.mobile.core.order.OrderFailureKind;
import br.com.tresvtintas.mobile.core.order.OrderListController;
import br.com.tresvtintas.mobile.core.order.OrderListState;
import br.com.tresvtintas.mobile.core.order.OrderListStateListener;
import br.com.tresvtintas.mobile.core.order.OrderQuery;
import br.com.tresvtintas.mobile.core.order.OrderView;
import br.com.tresvtintas.mobile.feature.order.databinding.OrderActivityListBinding;
import java.util.Optional;

public final class OrderListActivity extends AppCompatActivity {
    private static final long SEARCH_DELAY_MILLIS = 350;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final OrderListStateListener listener = this::render;
    private OrderActivityListBinding binding;
    private OrderListRenderer renderer;
    private Optional<OrderListController> controller = Optional.empty();
    private Optional<Runnable> pendingSearch = Optional.empty();

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        OrderPrivacy.protect(this);
        EdgeToEdge.enable(this);
        binding = OrderActivityListBinding.inflate(getLayoutInflater());
        OrderSummaryAdapter adapter = new OrderSummaryAdapter(this::openDetail);
        renderer = new OrderListRenderer(binding, adapter);
        setContentView(binding.getRoot());
        OrderInsets.applySystemBars(binding.getRoot());
        binding.orderList.setLayoutManager(new LinearLayoutManager(this));
        binding.orderList.setAdapter(adapter);
        binding.orderToolbar.setNavigationOnClickListener(ignored -> finish());
        binding.orderRefresh.setOnClickListener(ignored -> controller.ifPresent(OrderListController::refresh));
        binding.orderRetry.setOnClickListener(ignored -> retry());
        binding.orderLoadMore.setOnClickListener(ignored -> controller.ifPresent(OrderListController::loadMore));
        binding.orderViews.setOnCheckedStateChangeListener((group, checked) -> {
            if (!checked.isEmpty()) {
                changeView(checked.get(0));
            }
        });
        binding.orderSearch.addTextChangedListener(searchWatcher());
    }

    @Override
    protected void onStart() {
        super.onStart();
        Optional<OrderFeatureRuntime> runtime = runtime();
        if (runtime.isEmpty()) {
            render(OrderListState.error(new OrderException(OrderFailureKind.ACCESS_REVOKED,
                    "Order runtime is unavailable.")));
            return;
        }
        OrderFeatureRuntime value = runtime.orElseThrow();
        OrderListController next = new OrderListController(value.repository(), value.workerExecutor(),
                ContextCompat.getMainExecutor(this));
        controller = Optional.of(next);
        next.subscribe(listener);
        next.open(OrderQuery.initial().withSearch(text(binding.orderSearch.getText())));
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
        startActivity(OrderDetailActivity.intent(this, id));
    }

    private void retry() {
        controller.ifPresent(value -> value.open(value.currentQuery()));
    }

    private void changeView(int id) {
        OrderView view = id == R.id.order_view_history ? OrderView.HISTORY
                : id == R.id.order_view_all ? OrderView.ALL : OrderView.ACTIVE;
        controller.ifPresent(value -> value.open(value.currentQuery().withView(view)));
    }

    private TextWatcher searchWatcher() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence value, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence value, int start, int before, int count) {
                scheduleSearch(value == null ? "" : value.toString());
            }

            @Override
            public void afterTextChanged(Editable value) { }
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

    private void render(OrderListState state) {
        renderer.render(state);
    }

    private Optional<OrderFeatureRuntime> runtime() {
        return getApplication() instanceof OrderRuntimeProvider provider
                ? provider.orderRuntime()
                : Optional.empty();
    }

    private static String text(Editable value) {
        return value == null ? "" : value.toString();
    }
}
