package br.com.tresvtintas.mobile.feature.delivery;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import br.com.tresvtintas.mobile.core.delivery.DeliveryAction;
import br.com.tresvtintas.mobile.core.delivery.DeliveryActionController;
import br.com.tresvtintas.mobile.core.delivery.DeliveryActionState;
import br.com.tresvtintas.mobile.core.delivery.DeliveryDetail;
import br.com.tresvtintas.mobile.core.delivery.DeliveryDetailController;
import br.com.tresvtintas.mobile.core.delivery.DeliveryDetailState;
import br.com.tresvtintas.mobile.core.delivery.DeliveryException;
import br.com.tresvtintas.mobile.core.delivery.DeliveryFailureKind;
import br.com.tresvtintas.mobile.feature.delivery.databinding.DeliveryActivityDetailBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.Optional;

public final class DeliveryDetailActivity extends AppCompatActivity {
    private static final String EXTRA_ID =
            "br.com.tresvtintas.mobile.delivery.DELIVERY_ID";
    private static final String STATE_ACTION_KEY =
            "delivery_action_key";
    private static final String STATE_ACTION_FINGERPRINT =
            "delivery_action_fingerprint";
    private final DeliveryDetailController.Listener listener =
            this::render;
    private final DeliveryActionController.Listener actionListener =
            this::renderAction;
    private DeliveryActivityDetailBinding binding;
    private DeliveryDetailRenderer renderer;
    private Optional<DeliveryDetailController> controller =
            Optional.empty();
    private Optional<DeliveryActionController> actionController =
            Optional.empty();
    private Optional<DeliveryDetail> currentDetail =
            Optional.empty();
    private DeliveryActionAttempt actionAttempt =
            new DeliveryActionAttempt();
    private long deliveryId;

    public static Intent intent(Context context, long id) {
        return new Intent(context, DeliveryDetailActivity.class)
                .putExtra(EXTRA_ID, id);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        DeliveryPrivacy.protect(this);
        EdgeToEdge.enable(this);
        binding = DeliveryActivityDetailBinding.inflate(
                getLayoutInflater());
        renderer = new DeliveryDetailRenderer(binding);
        setContentView(binding.getRoot());
        DeliveryInsets.applySystemBars(binding.getRoot());
        deliveryId = getIntent().getLongExtra(EXTRA_ID, 0);
        binding.deliveryDetailToolbar.setNavigationOnClickListener(
                ignored -> finish());
        binding.deliveryDetailRetry.setOnClickListener(
                ignored -> load());
        binding.deliveryActionStart.setOnClickListener(
                ignored -> confirm(DeliveryAction.START));
        binding.deliveryActionComplete.setOnClickListener(
                ignored -> confirm(DeliveryAction.COMPLETE));
        if (state != null) {
            actionAttempt = DeliveryActionAttempt.restored(
                    state.getString(STATE_ACTION_KEY, ""),
                    state.getString(
                            STATE_ACTION_FINGERPRINT,
                            ""));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        state.putString(
                STATE_ACTION_KEY,
                actionAttempt.key());
        state.putString(
                STATE_ACTION_FINGERPRINT,
                actionAttempt.fingerprint());
        super.onSaveInstanceState(state);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Optional<DeliveryFeatureRuntime> runtime = runtime();
        if (runtime.isEmpty()) {
            render(DeliveryDetailState.error(new DeliveryException(
                    DeliveryFailureKind.ACCESS_REVOKED,
                    "Delivery runtime is unavailable.")));
            return;
        }
        DeliveryFeatureRuntime value = runtime.orElseThrow();
        DeliveryDetailController next =
                new DeliveryDetailController(
                        value.repository(),
                        value.workerExecutor(),
                        ContextCompat.getMainExecutor(this));
        DeliveryActionController nextAction =
                new DeliveryActionController(
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
        controller.ifPresent(value -> value.load(deliveryId));
    }

    private void render(DeliveryDetailState state) {
        if (state.phase() == DeliveryDetailState.Phase.READY) {
            currentDetail = state.delivery();
        } else if (state.phase()
                == DeliveryDetailState.Phase.ERROR) {
            currentDetail = Optional.empty();
        }
        renderer.render(state);
    }

    private void confirm(DeliveryAction action) {
        if (!authorized(action)) {
            return;
        }
        int title = action == DeliveryAction.START
                ? R.string.delivery_confirm_start_title
                : R.string.delivery_confirm_complete_title;
        int message = action == DeliveryAction.START
                ? R.string.delivery_confirm_start_message
                : R.string.delivery_confirm_complete_message;
        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton(
                        R.string.delivery_cancel,
                        null)
                .setPositiveButton(
                        R.string.delivery_confirm,
                        (dialog, ignored) -> execute(action))
                .show();
    }

    private void execute(DeliveryAction action) {
        DeliveryDetail detail = currentDetail.orElse(null);
        if (detail == null || !authorized(action)) {
            return;
        }
        int revision = detail.summary().order().revision();
        String key = actionAttempt.keyFor(
                action,
                deliveryId,
                revision);
        actionController.ifPresent(value -> value.execute(
                action,
                deliveryId,
                revision,
                key));
    }

    private void renderAction(DeliveryActionState state) {
        if (state.phase() == DeliveryActionState.Phase.RUNNING) {
            renderer.setActionsBusy(true);
            return;
        }
        renderer.setActionsBusy(false);
        if (state.phase() == DeliveryActionState.Phase.SUCCESS) {
            boolean replayed = state.result()
                    .orElseThrow()
                    .replayed();
            actionAttempt.reset();
            renderer.showActionNotice(
                    replayed
                            ? R.string.delivery_action_replayed
                            : R.string.delivery_action_success,
                    null);
            load();
        } else if (state.phase()
                == DeliveryActionState.Phase.ERROR) {
            DeliveryFailureKind failure =
                    state.failure().orElseThrow();
            if (failure == DeliveryFailureKind.CONFLICT
                    || failure
                            == DeliveryFailureKind.IDEMPOTENCY_KEY_REUSED) {
                actionAttempt.reset();
            }
            renderer.showActionNotice(
                    failure == DeliveryFailureKind.CONFLICT
                            ? R.string.delivery_action_conflict
                            : DeliveryText.failure(failure),
                    state.requestId().orElse(null));
            if (failure == DeliveryFailureKind.CONFLICT) {
                load();
            }
        }
    }

    private boolean authorized(DeliveryAction action) {
        return currentDetail
                .map(value -> value.summary()
                        .allowedActions()
                        .contains(action))
                .orElse(false);
    }

    private Optional<DeliveryFeatureRuntime> runtime() {
        return getApplication()
                        instanceof DeliveryRuntimeProvider provider
                ? provider.deliveryRuntime()
                : Optional.empty();
    }
}
