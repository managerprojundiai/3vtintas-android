package br.com.tresvtintas.mobile.feature.finance;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import br.com.tresvtintas.mobile.core.finance.FinanceAction;
import br.com.tresvtintas.mobile.core.finance.FinanceDraft;
import br.com.tresvtintas.mobile.core.finance.FinanceEntryType;
import br.com.tresvtintas.mobile.core.finance.FinanceMutationController;
import br.com.tresvtintas.mobile.core.finance.FinanceMutationState;
import br.com.tresvtintas.mobile.core.finance.FinanceMutationStateListener;
import br.com.tresvtintas.mobile.feature.finance.databinding.FinanceActivityCreateBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.OptionalLong;

public final class FinanceCreateActivity extends AppCompatActivity {
    private static final String STATE_KEY = "finance_create_key";
    private static final String STATE_FINGERPRINT = "finance_create_fingerprint";
    private final FinanceMutationStateListener listener = this::render;
    private FinanceActivityCreateBinding binding;
    private Optional<FinanceMutationController> controller = Optional.empty();
    private FinanceMutationAttempt attempt = new FinanceMutationAttempt();
    private FinanceRoute route = FinanceRoute.personal();

    public static Intent intent(Context context) {
        return intent(context, FinanceRoute.personal());
    }

    public static Intent intent(Context context, FinanceRoute route) {
        if (!route.canCreate()) {
            throw new IllegalArgumentException(
                    "Finance creation requires a writable scope.");
        }
        return route.apply(new Intent(context, FinanceCreateActivity.class));
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        FinancePrivacy.protect(this);
        EdgeToEdge.enable(this);
        route = FinanceRoute.from(getIntent());
        if (state != null) {
            attempt = FinanceMutationAttempt.restored(
                    state.getString(STATE_KEY),
                    state.getString(STATE_FINGERPRINT));
        }
        binding = FinanceActivityCreateBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        FinanceInsets.applySystemBars(binding.getRoot());
        if (route.experience()
                == br.com.tresvtintas.mobile.core.finance.FinanceExperience.CORPORATE) {
            binding.financeCreateToolbar.setTitle(
                    R.string.finance_corporate_create_title);
            binding.financeCreateToolbar.setSubtitle(
                    route.organizationName()
                            .map(value -> getString(
                                    R.string.finance_corporate_create_subtitle,
                                    value))
                            .orElseGet(() -> getString(
                                    R.string.finance_corporate_create_scope_missing)));
        }
        binding.financeCreateToolbar.setNavigationOnClickListener(
                ignored -> finish());
        binding.financeCreateSave.setOnClickListener(
                ignored -> review());
    }

    @Override
    protected void onStart() {
        super.onStart();
        Optional<FinanceFeatureRuntime> runtime = runtime();
        if (runtime.isEmpty()) {
            showError(getString(R.string.finance_error_session));
            binding.financeCreateSave.setEnabled(false);
            return;
        }
        FinanceFeatureRuntime value = runtime.orElseThrow();
        FinanceMutationController next = new FinanceMutationController(
                value.repository(),
                value.workerExecutor(),
                ContextCompat.getMainExecutor(this));
        controller = Optional.of(next);
        next.subscribe(listener);
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
    protected void onSaveInstanceState(Bundle state) {
        state.putString(STATE_KEY, attempt.key());
        state.putString(STATE_FINGERPRINT, attempt.fingerprint());
        super.onSaveInstanceState(state);
    }

    private void review() {
        try {
            FinanceDraft draft = draft();
            new MaterialAlertDialogBuilder(this)
                    .setTitle(route.experience()
                                    == br.com.tresvtintas.mobile.core.finance.FinanceExperience.CORPORATE
                            ? R.string.finance_corporate_create_confirm_title
                            : R.string.finance_create_confirm_title)
                    .setMessage(getString(
                            R.string.finance_create_confirm_message,
                            getString(FinanceText.type(draft.type())),
                            draft.title(),
                            FinanceText.money(draft.amount())))
                    .setNegativeButton(R.string.finance_cancel_action, null)
                    .setPositiveButton(
                            R.string.finance_confirm_create,
                            (ignoredDialog, ignoredButton) -> create(draft))
                    .show();
        } catch (IllegalArgumentException | DateTimeParseException failure) {
            showError(getString(R.string.finance_invalid_form));
        }
    }

    private void create(FinanceDraft draft) {
        String payload = draft.type().name()
                + "\n"
                + draft.title()
                + "\n"
                + draft.amount().toPlainString()
                + "\n"
                + draft.dueAt().map(Object::toString).orElse("")
                + "\n"
                + (draft.customerId().isPresent()
                        ? draft.customerId().orElseThrow()
                        : "")
                + "\n"
                + draft.notes().orElse("");
        controller.ifPresent(value -> value.create(
                draft,
                attempt.keyFor(FinanceAction.CREATE, 0, payload)));
    }

    private FinanceDraft draft() {
        String dueValue = text(binding.financeCreateDue.getText());
        Optional<java.time.Instant> dueAt = dueValue.isEmpty()
                ? Optional.empty()
                : Optional.of(LocalDate.parse(dueValue)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant());
        String notes = text(binding.financeCreateNotes.getText());
        return new FinanceDraft(
                selectedType(),
                text(binding.financeCreateTitleInput.getText()),
                new BigDecimal(text(binding.financeCreateAmount.getText())
                        .replace(',', '.')),
                dueAt,
                OptionalLong.empty(),
                notes.isEmpty() ? Optional.empty() : Optional.of(notes));
    }

    private FinanceEntryType selectedType() {
        int id = binding.financeCreateType.getCheckedChipId();
        if (id == R.id.finance_create_payable) {
            return FinanceEntryType.PAYABLE;
        }
        if (id == R.id.finance_create_receivable) {
            return FinanceEntryType.RECEIVABLE;
        }
        return FinanceEntryType.EXPENSE;
    }

    private void render(FinanceMutationState state) {
        boolean running = state.phase() == FinanceMutationState.Phase.RUNNING;
        binding.financeCreateProgress.setVisibility(
                running ? View.VISIBLE : View.INVISIBLE);
        binding.financeCreateSave.setEnabled(!running);
        if (state.phase() == FinanceMutationState.Phase.ERROR) {
            String message = FinanceText.failure(
                    this,
                    state.failure().orElseThrow());
            if (state.requestId().isPresent()) {
                message += "\n" + getString(
                        R.string.finance_support_code,
                        state.requestId().orElseThrow());
            }
            showError(message);
        }
        if (state.phase() == FinanceMutationState.Phase.SUCCESS) {
            boolean replayed = state.result().orElseThrow().replayed();
            attempt.reset();
            Toast.makeText(
                    this,
                    replayed
                            ? R.string.finance_mutation_replayed
                            : R.string.finance_mutation_success,
                    Toast.LENGTH_LONG)
                    .show();
            setResult(RESULT_OK);
            finish();
        }
    }

    private void showError(String message) {
        binding.financeCreateNotice.setText(message);
        binding.financeCreateNoticeCard.setVisibility(View.VISIBLE);
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
