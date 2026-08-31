package br.com.tresvtintas.mobile.feature.finance;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import br.com.tresvtintas.mobile.core.finance.FinanceAction;
import br.com.tresvtintas.mobile.core.finance.FinanceDetail;
import br.com.tresvtintas.mobile.core.finance.FinanceDetailController;
import br.com.tresvtintas.mobile.core.finance.FinanceDetailState;
import br.com.tresvtintas.mobile.core.finance.FinanceDetailStateListener;
import br.com.tresvtintas.mobile.core.finance.FinanceException;
import br.com.tresvtintas.mobile.core.finance.FinanceFailureKind;
import br.com.tresvtintas.mobile.core.finance.FinanceMutationController;
import br.com.tresvtintas.mobile.core.finance.FinanceMutationState;
import br.com.tresvtintas.mobile.core.finance.FinanceMutationStateListener;
import br.com.tresvtintas.mobile.core.finance.FinancePaymentMethod;
import br.com.tresvtintas.mobile.feature.finance.databinding.FinanceActivityDetailBinding;
import br.com.tresvtintas.mobile.feature.finance.databinding.FinanceDialogSettlementBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class FinanceDetailActivity extends AppCompatActivity {
    private static final long MINIMUM_ENTRY_ID = 1L;
    private static final String EXTRA_ENTRY_ID =
            "br.com.tresvtintas.mobile.finance.ENTRY_ID";
    private static final String STATE_KEY = "finance_mutation_key";
    private static final String STATE_FINGERPRINT = "finance_mutation_fingerprint";
    private final FinanceDetailStateListener detailListener = this::renderDetail;
    private final FinanceMutationStateListener mutationListener =
            this::renderMutation;
    private FinanceActivityDetailBinding binding;
    private FinanceDetailRenderer renderer;
    private long entryId;
    private Optional<FinanceDetail> currentDetail = Optional.empty();
    private Optional<FinanceDetailController> detailController = Optional.empty();
    private Optional<FinanceMutationController> mutationController =
            Optional.empty();
    private FinanceMutationAttempt attempt = new FinanceMutationAttempt();
    private FinanceRoute route = FinanceRoute.personal();

    public static Intent intent(Context context, long entryId) {
        return intent(context, entryId, FinanceRoute.personal());
    }

    public static Intent intent(
            Context context,
            long entryId,
            FinanceRoute route) {
        if (entryId < MINIMUM_ENTRY_ID) {
            throw new IllegalArgumentException("Finance entry ID is invalid.");
        }
        return route.apply(new Intent(context, FinanceDetailActivity.class)
                .putExtra(EXTRA_ENTRY_ID, entryId));
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        FinancePrivacy.protect(this);
        EdgeToEdge.enable(this);
        route = FinanceRoute.from(getIntent());
        entryId = getIntent().getLongExtra(EXTRA_ENTRY_ID, 0);
        if (state != null) {
            attempt = FinanceMutationAttempt.restored(
                    state.getString(STATE_KEY),
                    state.getString(STATE_FINGERPRINT));
        }
        binding = FinanceActivityDetailBinding.inflate(getLayoutInflater());
        renderer = new FinanceDetailRenderer(binding);
        setContentView(binding.getRoot());
        FinanceInsets.applySystemBars(binding.getRoot());
        binding.financeDetailToolbar.setSubtitle(
                route.experience()
                                == br.com.tresvtintas.mobile.core.finance.FinanceExperience.CORPORATE
                        ? R.string.finance_corporate_detail_subtitle
                        : R.string.finance_detail_subtitle);
        binding.financeDetailToolbar.setNavigationOnClickListener(
                ignored -> finish());
        binding.financeDetailRetry.setOnClickListener(ignored -> load());
        binding.financeDetailSettle.setOnClickListener(
                ignored -> showSettlement());
        binding.financeDetailCancel.setOnClickListener(
                ignored -> showCancellation());
    }

    @Override
    protected void onStart() {
        super.onStart();
        Optional<FinanceFeatureRuntime> runtime = runtime();
        if (runtime.isEmpty()) {
            renderDetail(FinanceDetailState.error(new FinanceException(
                    FinanceFailureKind.ACCESS_REVOKED,
                    "Finance runtime is unavailable.")));
            return;
        }
        FinanceFeatureRuntime value = runtime.orElseThrow();
        FinanceDetailController nextDetail = new FinanceDetailController(
                value.repository(),
                value.workerExecutor(),
                ContextCompat.getMainExecutor(this));
        FinanceMutationController nextMutation = new FinanceMutationController(
                value.repository(),
                value.workerExecutor(),
                ContextCompat.getMainExecutor(this));
        detailController = Optional.of(nextDetail);
        mutationController = Optional.of(nextMutation);
        nextDetail.subscribe(detailListener);
        nextMutation.subscribe(mutationListener);
        nextDetail.load(entryId);
    }

    @Override
    protected void onStop() {
        detailController.ifPresent(value -> {
            value.unsubscribe(detailListener);
            value.close();
        });
        mutationController.ifPresent(value -> {
            value.unsubscribe(mutationListener);
            value.close();
        });
        detailController = Optional.empty();
        mutationController = Optional.empty();
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        state.putString(STATE_KEY, attempt.key());
        state.putString(STATE_FINGERPRINT, attempt.fingerprint());
        super.onSaveInstanceState(state);
    }

    private void load() {
        detailController.ifPresent(value -> value.load(entryId));
    }

    private void showSettlement() {
        if (currentDetail
                .map(FinanceDetail::summary)
                .filter(value -> value.allowedActions()
                        .contains(FinanceAction.SETTLE))
                .isEmpty()) {
            return;
        }
        FinanceDialogSettlementBinding dialog =
                FinanceDialogSettlementBinding.inflate(getLayoutInflater());
        List<FinancePaymentMethod> methods = List.of(
                FinancePaymentMethod.PIX,
                FinancePaymentMethod.TRANSFER,
                FinancePaymentMethod.CASH,
                FinancePaymentMethod.BANK_SLIP,
                FinancePaymentMethod.OTHER);
        List<String> labels = new ArrayList<>(methods.size());
        for (FinancePaymentMethod method : methods) {
            labels.add(getString(FinanceText.payment(method)));
        }
        dialog.financePaymentMethod.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                labels));
        dialog.financePaymentMethod.setText(labels.get(0), false);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.finance_settlement_title)
                .setMessage(R.string.finance_settlement_message)
                .setView(dialog.getRoot())
                .setNegativeButton(R.string.finance_cancel_action, null)
                .setPositiveButton(
                        R.string.finance_confirm_settlement,
                        (ignoredDialog, ignoredButton) -> {
                            int index = Math.max(
                                    0,
                                    labels.indexOf(dialog.financePaymentMethod
                                            .getText()
                                            .toString()));
                            FinancePaymentMethod method = methods.get(index);
                            String reference = text(
                                    dialog.financePaymentReference.getText());
                            String payload = method.name() + "\n" + reference;
                            mutationController.ifPresent(value -> value.settle(
                                    entryId,
                                    method,
                                    Optional.ofNullable(reference)
                                            .filter(item -> !item.isBlank()),
                                    attempt.keyFor(
                                            FinanceAction.SETTLE,
                                            entryId,
                                            payload)));
                        })
                .show();
    }

    private void showCancellation() {
        if (currentDetail
                .map(FinanceDetail::summary)
                .filter(value -> value.allowedActions()
                        .contains(FinanceAction.CANCEL))
                .isEmpty()) {
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.finance_cancel_title)
                .setMessage(R.string.finance_cancel_message)
                .setNegativeButton(R.string.finance_cancel_action, null)
                .setPositiveButton(
                        R.string.finance_confirm_cancel,
                        (ignoredDialog, ignoredButton) ->
                                mutationController.ifPresent(value ->
                                        value.cancel(
                                                entryId,
                                                attempt.keyFor(
                                                        FinanceAction.CANCEL,
                                                        entryId,
                                                        ""))))
                .show();
    }

    private void renderDetail(FinanceDetailState state) {
        state.detail().ifPresent(value -> currentDetail = Optional.of(value));
        renderer.render(state);
    }

    private void renderMutation(FinanceMutationState state) {
        renderer.renderMutation(state);
        if (state.phase() == FinanceMutationState.Phase.SUCCESS) {
            boolean replayed = state.result().orElseThrow().replayed();
            attempt.reset();
            renderer.success(replayed);
            load();
        }
    }

    private Optional<FinanceFeatureRuntime> runtime() {
        return getApplication() instanceof FinanceRuntimeProvider provider
                ? provider.financeRuntime(route)
                : Optional.empty();
    }

    private static String text(android.text.Editable value) {
        return value == null ? "" : value.toString().trim();
    }
}
