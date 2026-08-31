package br.com.tresvtintas.mobile.feature.order;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import br.com.tresvtintas.mobile.core.order.OrderDetailController;
import br.com.tresvtintas.mobile.core.order.OrderAction;
import br.com.tresvtintas.mobile.core.order.OrderActionController;
import br.com.tresvtintas.mobile.core.order.OrderActionState;
import br.com.tresvtintas.mobile.core.order.OrderActionStateListener;
import br.com.tresvtintas.mobile.core.order.OrderDetail;
import br.com.tresvtintas.mobile.core.order.OrderDetailState;
import br.com.tresvtintas.mobile.core.order.OrderDetailStateListener;
import br.com.tresvtintas.mobile.core.order.OrderException;
import br.com.tresvtintas.mobile.core.order.OrderFailureKind;
import br.com.tresvtintas.mobile.core.order.OrderPaymentMethod;
import br.com.tresvtintas.mobile.core.order.OrderStatus;
import br.com.tresvtintas.mobile.feature.order.databinding.OrderActivityDetailBinding;
import java.util.Optional;

public final class OrderDetailActivity extends AppCompatActivity {
    private static final String EXTRA_ID = "br.com.tresvtintas.mobile.order.ORDER_ID";
    private static final String STATE_ACTION_KEY = "order_action_key";
    private static final String STATE_ACTION_FINGERPRINT =
            "order_action_fingerprint";
    private final OrderDetailStateListener listener = this::render;
    private final OrderActionStateListener actionListener =
            this::renderAction;
    private OrderActivityDetailBinding binding;
    private OrderDetailRenderer renderer;
    private OrderActionDialogs dialogs;
    private Optional<OrderDetailController> controller = Optional.empty();
    private Optional<OrderActionController> actionController =
            Optional.empty();
    private Optional<OrderDetail> currentDetail = Optional.empty();
    private OrderActionAttempt actionAttempt = new OrderActionAttempt();
    private long orderId;

    public static Intent intent(Context context, long id) {
        return new Intent(context, OrderDetailActivity.class).putExtra(EXTRA_ID, id);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        OrderPrivacy.protect(this);
        EdgeToEdge.enable(this);
        binding = OrderActivityDetailBinding.inflate(getLayoutInflater());
        renderer = new OrderDetailRenderer(binding);
        dialogs = new OrderActionDialogs(this);
        setContentView(binding.getRoot());
        OrderInsets.applySystemBars(binding.getRoot());
        orderId = getIntent().getLongExtra(EXTRA_ID, 0);
        binding.orderDetailToolbar.setNavigationOnClickListener(ignored -> finish());
        binding.orderDetailRetry.setOnClickListener(ignored -> load());
        binding.orderActionConfirm.setOnClickListener(
                ignored -> confirmStatus(OrderAction.CONFIRM));
        binding.orderActionStart.setOnClickListener(
                ignored -> confirmStatus(OrderAction.START_FULFILLMENT));
        binding.orderActionComplete.setOnClickListener(
                ignored -> confirmStatus(OrderAction.COMPLETE));
        binding.orderActionCancel.setOnClickListener(
                ignored -> confirmCancellation());
        binding.orderActionPayment.setOnClickListener(
                ignored -> confirmPayment());
        if (state != null) {
            actionAttempt = OrderActionAttempt.restored(
                    state.getString(STATE_ACTION_KEY, ""),
                    state.getString(STATE_ACTION_FINGERPRINT, ""));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        state.putString(STATE_ACTION_KEY, actionAttempt.key());
        state.putString(
                STATE_ACTION_FINGERPRINT,
                actionAttempt.fingerprint());
        super.onSaveInstanceState(state);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Optional<OrderFeatureRuntime> runtime = runtime();
        if (runtime.isEmpty()) {
            render(OrderDetailState.error(new OrderException(OrderFailureKind.ACCESS_REVOKED,
                    "Order runtime is unavailable.")));
            return;
        }
        OrderFeatureRuntime value = runtime.orElseThrow();
        OrderDetailController next = new OrderDetailController(value.repository(), value.workerExecutor(),
                ContextCompat.getMainExecutor(this));
        OrderActionController nextAction = new OrderActionController(
                value.repository(),
                value.workerExecutor(),
                ContextCompat.getMainExecutor(this));
        controller = Optional.of(next);
        actionController = Optional.of(nextAction);
        next.subscribe(listener);
        nextAction.subscribe(actionListener);
        load();
    }

    @Override
    protected void onStop() {
        controller.ifPresent(value -> {
            value.unsubscribe(listener);
            value.close();
        });
        controller = Optional.empty();
        actionController.ifPresent(value -> {
            value.unsubscribe(actionListener);
            value.close();
        });
        actionController = Optional.empty();
        currentDetail = Optional.empty();
        super.onStop();
    }

    private void load() {
        controller.ifPresent(value -> value.load(orderId));
    }

    private void render(OrderDetailState state) {
        if (state.phase() == OrderDetailState.Phase.READY) {
            currentDetail = state.order();
        } else if (state.phase() == OrderDetailState.Phase.ERROR) {
            currentDetail = Optional.empty();
        }
        renderer.render(state);
    }

    private void confirmStatus(OrderAction action) {
        if (!authorized(action)) {
            return;
        }
        dialogs.confirmStatus(action, () -> transitionStatus(action));
    }

    private void transitionStatus(OrderAction action) {
        OrderDetail detail = currentDetail.orElse(null);
        OrderStatus status = targetStatus(action);
        if (detail == null || status == null || !authorized(action)) {
            return;
        }
        String key = actionAttempt.keyFor(
                action,
                orderId,
                detail.summary().revision(),
                status.name());
        actionController.ifPresent(value -> value.transitionStatus(
                orderId,
                detail.summary().revision(),
                status,
                key));
    }

    private void confirmCancellation() {
        if (authorized(OrderAction.CANCEL)) {
            dialogs.confirmCancellation(this::cancel);
        }
    }

    private void cancel(Optional<String> reason) {
        OrderDetail detail = currentDetail.orElse(null);
        if (detail == null || !authorized(OrderAction.CANCEL)) {
            return;
        }
        String normalized = reason.orElse("").trim();
        String key = actionAttempt.keyFor(
                OrderAction.CANCEL,
                orderId,
                detail.summary().revision(),
                normalized);
        actionController.ifPresent(value -> value.cancel(
                orderId,
                detail.summary().revision(),
                Optional.of(normalized),
                key));
    }

    private void confirmPayment() {
        if (authorized(OrderAction.RECORD_PAYMENT)) {
            dialogs.confirmPayment(this::recordPayment);
        }
    }

    private void recordPayment(
            OrderPaymentMethod method,
            Optional<String> reference) {
        OrderDetail detail = currentDetail.orElse(null);
        if (detail == null || !authorized(OrderAction.RECORD_PAYMENT)) {
            return;
        }
        String normalized = reference.orElse("").trim();
        String key = actionAttempt.keyFor(
                OrderAction.RECORD_PAYMENT,
                orderId,
                detail.summary().revision(),
                method.name() + "\n" + normalized);
        actionController.ifPresent(value -> value.recordPayment(
                orderId,
                detail.summary().revision(),
                method,
                Optional.of(normalized),
                key));
    }

    private void renderAction(OrderActionState state) {
        if (state.phase() == OrderActionState.Phase.RUNNING) {
            renderer.setActionsBusy(true);
            return;
        }
        renderer.setActionsBusy(false);
        if (state.phase() == OrderActionState.Phase.SUCCESS) {
            boolean replayed = state.result().orElseThrow().replayed();
            actionAttempt.reset();
            renderer.showActionNotice(
                    replayed
                            ? R.string.order_action_replayed
                            : R.string.order_action_success,
                    null);
            load();
        } else if (state.phase() == OrderActionState.Phase.ERROR) {
            OrderFailureKind failure = state.failure().orElseThrow();
            if (failure == OrderFailureKind.CONFLICT
                    || failure == OrderFailureKind.IDEMPOTENCY_KEY_REUSED) {
                actionAttempt.reset();
            }
            renderer.showActionNotice(
                    failure == OrderFailureKind.CONFLICT
                            ? R.string.order_action_conflict
                            : OrderText.failure(failure),
                    state.requestId().orElse(null));
            if (failure == OrderFailureKind.CONFLICT) {
                load();
            }
        }
    }

    private boolean authorized(OrderAction action) {
        return currentDetail
                .map(value -> value.summary().allowedActions().contains(action))
                .orElse(false);
    }

    private static OrderStatus targetStatus(OrderAction action) {
        return switch (action) {
            case CONFIRM -> OrderStatus.CONFIRMED;
            case START_FULFILLMENT -> OrderStatus.IN_PROGRESS;
            case COMPLETE -> OrderStatus.DELIVERED;
            default -> null;
        };
    }

    private Optional<OrderFeatureRuntime> runtime() {
        return getApplication() instanceof OrderRuntimeProvider provider
                ? provider.orderRuntime()
                : Optional.empty();
    }
}
