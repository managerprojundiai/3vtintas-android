package br.com.tresvtintas.mobile.feature.appointment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import br.com.tresvtintas.mobile.core.appointment.AppointmentDetail;
import br.com.tresvtintas.mobile.core.appointment.AppointmentDetailController;
import br.com.tresvtintas.mobile.core.appointment.AppointmentDetailState;
import br.com.tresvtintas.mobile.core.appointment.AppointmentDraft;
import br.com.tresvtintas.mobile.core.appointment.AppointmentEdit;
import br.com.tresvtintas.mobile.core.appointment.AppointmentMutationController;
import br.com.tresvtintas.mobile.core.appointment.AppointmentMutationAction;
import br.com.tresvtintas.mobile.core.appointment.AppointmentMutationState;
import br.com.tresvtintas.mobile.core.appointment.AppointmentResponsibleState;
import br.com.tresvtintas.mobile.core.customer.CustomerListState;
import br.com.tresvtintas.mobile.feature.appointment.databinding.AppointmentActivityFormBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.Optional;

public final class AppointmentFormActivity extends AppCompatActivity
        implements AppointmentFormDirectory.Listener {
    private static final String EXTRA_APPOINTMENT_ID =
            "br.com.tresvtintas.mobile.extra.EDIT_APPOINTMENT_ID";
    private static final String STATE_KEY = "appointment_form_key";
    private static final String STATE_FINGERPRINT =
            "appointment_form_fingerprint";
    private AppointmentActivityFormBinding binding;
    private AppointmentFormBinder binder;
    private Optional<AppointmentDetailController> detailController =
            Optional.empty();
    private Optional<AppointmentMutationController> mutationController =
            Optional.empty();
    private Optional<AppointmentFormDirectory> directory = Optional.empty();
    private Optional<AppointmentDetail> detail = Optional.empty();
    private AppointmentMutationAttempt attempt =
            new AppointmentMutationAttempt();
    private long appointmentId;
    private boolean formInitialized;

    public static Intent createIntent(Context context) {
        return new Intent(context, AppointmentFormActivity.class);
    }

    public static Intent editIntent(Context context, long appointmentId) {
        return new Intent(context, AppointmentFormActivity.class)
                .putExtra(EXTRA_APPOINTMENT_ID, appointmentId);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        AppointmentPrivacy.protect(this);
        EdgeToEdge.enable(this);
        if (state != null) {
            attempt = AppointmentMutationAttempt.restored(
                    state.getString(STATE_KEY),
                    state.getString(STATE_FINGERPRINT));
        }
        binding = AppointmentActivityFormBinding.inflate(
                getLayoutInflater());
        binder = new AppointmentFormBinder(this, binding);
        if (state != null) {
            binder.restoreState(state);
            formInitialized = true;
        }
        setContentView(binding.getRoot());
        AppointmentInsets.applySystemBars(binding.getRoot());
        appointmentId = getIntent().getLongExtra(
                EXTRA_APPOINTMENT_ID,
                -1L);
        binding.appointmentFormToolbar.setTitle(
                editing()
                        ? R.string.appointment_form_edit_title
                        : R.string.appointment_form_create_title);
        binding.appointmentFormToolbar.setNavigationOnClickListener(
                ignored -> finish());
        binding.appointmentFormSave.setOnClickListener(
                ignored -> review());
    }

    @Override
    protected void onStart() {
        super.onStart();
        Optional<AppointmentFeatureRuntime> runtime = runtime();
        if (runtime.isEmpty() || !runtime.orElseThrow().canWrite()) {
            finish();
            return;
        }
        AppointmentFeatureRuntime value = runtime.orElseThrow();
        binder.configure(value, editing());
        AppointmentMutationController mutations =
                new AppointmentMutationController(
                        value.repository(),
                        value.workerExecutor(),
                        ContextCompat.getMainExecutor(this));
        mutationController = Optional.of(mutations);
        mutations.subscribe(this::renderMutation);
        AppointmentFormDirectory nextDirectory =
                new AppointmentFormDirectory(
                        value,
                        ContextCompat.getMainExecutor(this),
                        this);
        directory = Optional.of(nextDirectory);
        nextDirectory.open();
        if (editing()) {
            AppointmentDetailController details =
                    new AppointmentDetailController(
                            value.repository(),
                            value.workerExecutor(),
                            ContextCompat.getMainExecutor(this));
            detailController = Optional.of(details);
            details.subscribe(this::renderDetail);
            details.load(appointmentId, value.scope());
        }
    }

    @Override
    protected void onStop() {
        detailController.ifPresent(AppointmentDetailController::close);
        mutationController.ifPresent(
                AppointmentMutationController::close);
        directory.ifPresent(AppointmentFormDirectory::close);
        detailController = Optional.empty();
        mutationController = Optional.empty();
        directory = Optional.empty();
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        state.putString(STATE_KEY, attempt.key());
        state.putString(STATE_FINGERPRINT, attempt.fingerprint());
        binder.saveState(state);
        super.onSaveInstanceState(state);
    }

    @Override
    public void onResponsibleState(AppointmentResponsibleState state) {
        if (state.snapshot().isPresent()) {
            binder.responsibles(
                    state.snapshot().orElseThrow().items());
        }
        if (state.phase() == AppointmentResponsibleState.Phase.ERROR) {
            notice(AppointmentText.failure(
                    state.failure().orElseThrow()));
        }
    }

    @Override
    public void onCustomerState(CustomerListState state) {
        if (state.snapshot().isPresent()) {
            binder.customers(state.snapshot().orElseThrow().items());
        }
        if (state.phase() == CustomerListState.Phase.ERROR) {
            notice(R.string.appointment_failure_service);
        }
    }

    private void renderDetail(AppointmentDetailState state) {
        busy(state.phase() == AppointmentDetailState.Phase.LOADING);
        if (state.phase() == AppointmentDetailState.Phase.ERROR) {
            notice(AppointmentText.failure(
                    state.failure().orElseThrow()));
            return;
        }
        state.detail().ifPresent(value -> {
            detail = Optional.of(value);
            if (!formInitialized) {
                binder.bind(value);
                formInitialized = true;
            }
        });
    }

    private void renderMutation(AppointmentMutationState state) {
        busy(state.phase() == AppointmentMutationState.Phase.RUNNING);
        if (state.phase() == AppointmentMutationState.Phase.ERROR) {
            notice(AppointmentText.failure(
                    state.failure().orElseThrow()));
            state.requestId().ifPresent(value ->
                    binding.appointmentFormNotice.append(
                            "\n"
                                    + getString(
                                            R.string.appointment_support_code,
                                            value)));
        } else if (state.phase()
                == AppointmentMutationState.Phase.SUCCESS) {
            attempt.reset();
            setResult(RESULT_OK);
            finish();
        }
    }

    private void review() {
        Optional<AppointmentFeatureRuntime> runtime = runtime();
        if (runtime.isEmpty()) {
            return;
        }
        binding.appointmentFormNoticeCard.setVisibility(View.GONE);
        try {
            if (editing()) {
                AppointmentDetail current = detail.orElseThrow(() ->
                        new AppointmentFormBinder.FormException(
                                R.string.appointment_failure_conflict));
                AppointmentEdit edit = binder.edit(
                        runtime.orElseThrow(),
                        current.summary().revision());
                new MaterialAlertDialogBuilder(this)
                        .setTitle(
                                R.string.appointment_form_update_confirm_title)
                        .setMessage(getString(
                                R.string.appointment_form_update_confirm_message,
                                edit.title()))
                        .setNegativeButton(
                                R.string.appointment_action_keep,
                                null)
                        .setPositiveButton(
                                R.string.appointment_action_apply,
                                (dialog, which) -> update(edit))
                        .show();
            } else {
                AppointmentDraft draft =
                        binder.draft(runtime.orElseThrow());
                new MaterialAlertDialogBuilder(this)
                        .setTitle(
                                R.string.appointment_form_create_confirm_title)
                        .setMessage(getString(
                                R.string.appointment_form_create_confirm_message,
                                draft.title()))
                        .setNegativeButton(
                                R.string.appointment_action_keep,
                                null)
                        .setPositiveButton(
                                R.string.appointment_action_apply,
                                (dialog, which) -> create(draft))
                        .show();
            }
        } catch (AppointmentFormBinder.FormException exception) {
            notice(exception.messageResource());
        } catch (IllegalArgumentException exception) {
            notice(R.string.appointment_form_required);
        }
    }

    private void create(AppointmentDraft draft) {
        mutationController.ifPresent(controller -> controller.create(
                draft,
                attempt.keyFor(
                        AppointmentMutationAction.CREATE,
                        0,
                        0,
                        binder.fingerprint(draft))));
    }

    private void update(AppointmentEdit edit) {
        mutationController.ifPresent(controller -> controller.update(
                appointmentId,
                edit,
                attempt.keyFor(
                        AppointmentMutationAction.UPDATE,
                        appointmentId,
                        edit.expectedRevision(),
                        binder.fingerprint(edit))));
    }

    private void busy(boolean value) {
        binding.appointmentFormProgress.setVisibility(
                value ? View.VISIBLE : View.INVISIBLE);
        binding.appointmentFormSave.setEnabled(!value);
    }

    private void notice(int message) {
        binding.appointmentFormNotice.setText(message);
        binding.appointmentFormNoticeCard.setVisibility(View.VISIBLE);
    }

    private boolean editing() {
        return appointmentId > 0;
    }

    private Optional<AppointmentFeatureRuntime> runtime() {
        return getApplication() instanceof AppointmentRuntimeProvider provider
                ? provider.appointmentRuntime()
                : Optional.empty();
    }
}
