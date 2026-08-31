package br.com.tresvtintas.mobile.feature.commission;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import androidx.appcompat.app.AlertDialog;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import br.com.tresvtintas.mobile.core.commission.CommissionAction;
import br.com.tresvtintas.mobile.core.commission.CommissionActionController;
import br.com.tresvtintas.mobile.core.commission.CommissionActionState;
import br.com.tresvtintas.mobile.core.commission.CommissionDetail;
import br.com.tresvtintas.mobile.core.commission.CommissionDetailController;
import br.com.tresvtintas.mobile.core.commission.CommissionDetailState;
import br.com.tresvtintas.mobile.core.commission.CommissionDetailStateListener;
import br.com.tresvtintas.mobile.core.commission.CommissionException;
import br.com.tresvtintas.mobile.core.commission.CommissionFailureKind;
import br.com.tresvtintas.mobile.core.commission.CommissionMutationCommand;
import br.com.tresvtintas.mobile.core.commission.CommissionPaymentMethod;
import br.com.tresvtintas.mobile.core.commission.CommissionScope;
import br.com.tresvtintas.mobile.feature.commission.databinding.CommissionActivityDetailBinding;
import br.com.tresvtintas.mobile.feature.commission.databinding.CommissionDialogCancellationBinding;
import br.com.tresvtintas.mobile.feature.commission.databinding.CommissionDialogPaymentBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.Optional;

public final class CommissionDetailActivity extends AppCompatActivity {
    private static final String EXTRA_ID =
            "br.com.tresvtintas.mobile.commission.ID";
    private static final String EXTRA_SCOPE =
            "br.com.tresvtintas.mobile.commission.DETAIL_SCOPE";
    private static final String STATE_ACTION_KEY =
            "commission_action_key";
    private static final String STATE_ACTION_FINGERPRINT =
            "commission_action_fingerprint";
    private final CommissionDetailStateListener listener = this::render;
    private final CommissionActionController.Listener actionListener =
            this::renderAction;
    private CommissionActivityDetailBinding binding;
    private CommissionDetailRenderer renderer;
    private long commissionId;
    private CommissionScope scope;
    private Optional<CommissionDetailController> controller = Optional.empty();
    private Optional<CommissionActionController> actionController =
            Optional.empty();
    private Optional<CommissionDetail> currentDetail = Optional.empty();
    private CommissionActionAttempt actionAttempt =
            new CommissionActionAttempt();

    public static Intent intent(
            Context context,
            long commissionId,
            CommissionScope scope) {
        if (commissionId < 1 || scope == null) {
            throw new IllegalArgumentException(
                    "Commission detail input is invalid.");
        }
        return new Intent(context, CommissionDetailActivity.class)
                .putExtra(EXTRA_ID, commissionId)
                .putExtra(EXTRA_SCOPE, scope.queryValue());
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        CommissionPrivacy.protect(this);
        EdgeToEdge.enable(this);
        commissionId = getIntent().getLongExtra(EXTRA_ID, 0);
        scope = CommissionScope.TEAM.queryValue().equals(
                getIntent().getStringExtra(EXTRA_SCOPE))
                        ? CommissionScope.TEAM
                        : CommissionScope.SELF;
        binding = CommissionActivityDetailBinding.inflate(
                getLayoutInflater());
        renderer = new CommissionDetailRenderer(binding);
        setContentView(binding.getRoot());
        CommissionInsets.applySystemBars(binding.getRoot());
        binding.commissionDetailToolbar.setNavigationOnClickListener(
                ignored -> finish());
        binding.commissionDetailRetry.setOnClickListener(
                ignored -> load());
        binding.commissionActionApprove.setOnClickListener(
                ignored -> confirmApproval());
        binding.commissionActionCancel.setOnClickListener(
                ignored -> confirmCancellation());
        binding.commissionActionPay.setOnClickListener(
                ignored -> confirmPayment());
        if (state != null) {
            actionAttempt = CommissionActionAttempt.restored(
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
        Optional<CommissionFeatureRuntime> runtime = runtime();
        if (runtime.isEmpty()) {
            render(CommissionDetailState.error(new CommissionException(
                    CommissionFailureKind.ACCESS_REVOKED,
                    "Commission runtime is unavailable.")));
            return;
        }
        CommissionFeatureRuntime value = runtime.orElseThrow();
        CommissionDetailController next = new CommissionDetailController(
                value.repository(),
                value.workerExecutor(),
                ContextCompat.getMainExecutor(this));
        CommissionActionController nextAction =
                new CommissionActionController(
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
        controller.ifPresent(value -> value.load(commissionId, scope));
    }

    private void render(CommissionDetailState state) {
        if (state.phase() == CommissionDetailState.Phase.READY) {
            currentDetail = state.detail();
        } else if (state.phase() == CommissionDetailState.Phase.ERROR) {
            currentDetail = Optional.empty();
        }
        renderer.render(state);
    }

    private void confirmApproval() {
        CommissionDetail detail = authorized(CommissionAction.APPROVE);
        if (detail == null) {
            return;
        }
        CommissionMutationCommand command =
                CommissionMutationCommand.approve(
                        detail.summary().id(),
                        detail.summary().revision());
        String recipient = detail.summary().recipient().name()
                .orElseGet(() -> getString(
                        R.string.commission_not_informed));
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.commission_action_approve_title)
                .setMessage(getString(
                        R.string.commission_action_approve_message,
                        detail.summary().id(),
                        recipient,
                        CommissionText.money(
                                detail.summary().calculation().amount())))
                .setNegativeButton(
                        R.string.commission_action_dismiss,
                        null)
                .setPositiveButton(
                        R.string.commission_action_confirm,
                        (dialog, ignored) -> execute(command))
                .show();
    }

    private void confirmCancellation() {
        CommissionDetail detail = authorized(CommissionAction.CANCEL);
        if (detail == null) {
            return;
        }
        CommissionDialogCancellationBinding form =
                CommissionDialogCancellationBinding.inflate(
                        getLayoutInflater());
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.commission_action_cancel_title)
                .setMessage(R.string.commission_action_cancel_message)
                .setView(form.getRoot())
                .setNegativeButton(
                        R.string.commission_action_dismiss,
                        null)
                .setPositiveButton(
                        R.string.commission_action_confirm,
                        null)
                .create();
        dialog.setOnShowListener(ignored -> dialog
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(button -> {
                    String reason = text(form.commissionCancellationReason);
                    try {
                        CommissionMutationCommand command =
                                CommissionMutationCommand.cancel(
                                        detail.summary().id(),
                                        detail.summary().revision(),
                                        reason);
                        form.commissionCancellationReasonLayout.setError(null);
                        dialog.dismiss();
                        execute(command);
                    } catch (IllegalArgumentException exception) {
                        form.commissionCancellationReasonLayout.setError(
                                getString(
                                        R.string
                                                .commission_cancellation_reason_error));
                    }
                }));
        dialog.show();
    }

    private void confirmPayment() {
        CommissionDetail detail = authorized(CommissionAction.PAY);
        if (detail == null) {
            return;
        }
        CommissionDialogPaymentBinding form =
                CommissionDialogPaymentBinding.inflate(getLayoutInflater());
        String[] labels = paymentMethodLabels();
        form.commissionPaymentMethod.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                labels));
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.commission_action_pay_title)
                .setMessage(getString(
                        R.string.commission_action_pay_message,
                        detail.summary().id(),
                        CommissionText.money(
                                detail.summary().calculation().amount())))
                .setView(form.getRoot())
                .setNegativeButton(
                        R.string.commission_action_dismiss,
                        null)
                .setPositiveButton(
                        R.string.commission_action_confirm,
                        null)
                .create();
        dialog.setOnShowListener(ignored -> dialog
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(button -> {
                    Optional<CommissionPaymentMethod> method =
                            paymentMethod(text(form.commissionPaymentMethod));
                    if (method.isEmpty()) {
                        form.commissionPaymentMethodLayout.setError(
                                getString(
                                        R.string
                                                .commission_payment_method_error));
                        return;
                    }
                    CommissionMutationCommand command =
                            CommissionMutationCommand.pay(
                                    detail.summary().id(),
                                    detail.summary().revision(),
                                    method.orElseThrow(),
                                    text(form.commissionPaymentReference));
                    form.commissionPaymentMethodLayout.setError(null);
                    dialog.dismiss();
                    execute(command);
                }));
        dialog.show();
    }

    private void execute(CommissionMutationCommand command) {
        CommissionDetail detail = authorized(command.action());
        if (detail == null
                || detail.summary().id() != command.commissionId()
                || detail.summary().revision()
                        != command.expectedRevision()) {
            return;
        }
        String key = actionAttempt.keyFor(command);
        actionController.ifPresent(value -> value.execute(command, key));
    }

    private void renderAction(CommissionActionState state) {
        if (state.phase() == CommissionActionState.Phase.RUNNING) {
            renderer.setActionsBusy(true);
            return;
        }
        renderer.setActionsBusy(false);
        if (state.phase() == CommissionActionState.Phase.SUCCESS) {
            boolean replayed = state.result().orElseThrow().replayed();
            actionAttempt.reset();
            renderer.showActionNotice(
                    replayed
                            ? R.string.commission_action_replayed
                            : R.string.commission_action_success,
                    null);
            load();
        } else if (state.phase() == CommissionActionState.Phase.ERROR) {
            CommissionFailureKind failure = state.failure().orElseThrow();
            if (failure == CommissionFailureKind.CONFLICT
                    || failure
                            == CommissionFailureKind.IDEMPOTENCY_KEY_REUSED) {
                actionAttempt.reset();
            }
            renderer.showActionNotice(
                    CommissionText.failure(failure),
                    state.requestId().orElse(null));
            if (failure == CommissionFailureKind.CONFLICT) {
                load();
            }
        }
    }

    private CommissionDetail authorized(CommissionAction action) {
        return currentDetail
                .filter(value -> value.summary()
                        .allowedActions()
                        .contains(action))
                .orElse(null);
    }

    private String[] paymentMethodLabels() {
        return new String[] {
            getString(R.string.commission_payment_pix),
            getString(R.string.commission_payment_transfer),
            getString(R.string.commission_payment_cash),
            getString(R.string.commission_payment_bank_slip),
            getString(R.string.commission_payment_other)
        };
    }

    private Optional<CommissionPaymentMethod> paymentMethod(String label) {
        CommissionPaymentMethod[] methods = CommissionPaymentMethod.values();
        String[] labels = paymentMethodLabels();
        for (int index = 0; index < labels.length; index++) {
            if (labels[index].equals(label)) {
                return Optional.of(methods[index]);
            }
        }
        return Optional.empty();
    }

    private static String text(android.widget.TextView view) {
        return view.getText() == null
                ? ""
                : view.getText().toString().trim();
    }

    private Optional<CommissionFeatureRuntime> runtime() {
        return getApplication() instanceof CommissionRuntimeProvider provider
                ? provider.commissionRuntime()
                : Optional.empty();
    }
}
