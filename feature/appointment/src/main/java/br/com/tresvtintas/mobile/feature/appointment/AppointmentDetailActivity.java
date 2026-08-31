package br.com.tresvtintas.mobile.feature.appointment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import br.com.tresvtintas.mobile.core.appointment.AppointmentDetail;
import br.com.tresvtintas.mobile.core.appointment.AppointmentDetailController;
import br.com.tresvtintas.mobile.core.appointment.AppointmentDetailState;
import br.com.tresvtintas.mobile.core.appointment.AppointmentMutationController;
import br.com.tresvtintas.mobile.core.appointment.AppointmentMutationAction;
import br.com.tresvtintas.mobile.core.appointment.AppointmentMutationState;
import br.com.tresvtintas.mobile.core.appointment.AppointmentStatus;
import br.com.tresvtintas.mobile.feature.appointment.databinding.AppointmentActivityDetailBinding;
import java.util.Optional;

public final class AppointmentDetailActivity extends AppCompatActivity {
    private static final String EXTRA_APPOINTMENT_ID =
            "br.com.tresvtintas.mobile.extra.APPOINTMENT_ID";
    private static final String STATE_KEY = "appointment_detail_key";
    private static final String STATE_FINGERPRINT =
            "appointment_detail_fingerprint";
    private AppointmentActivityDetailBinding binding;
    private AppointmentDetailRenderer renderer;
    private Optional<AppointmentDetailController> detailController =
            Optional.empty();
    private Optional<AppointmentMutationController> mutationController =
            Optional.empty();
    private Optional<AppointmentDetail> current = Optional.empty();
    private AppointmentMutationAttempt attempt =
            new AppointmentMutationAttempt();
    private long appointmentId;

    public static Intent intent(Context context, long appointmentId) {
        return new Intent(context, AppointmentDetailActivity.class)
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
        binding = AppointmentActivityDetailBinding.inflate(
                getLayoutInflater());
        renderer = new AppointmentDetailRenderer(binding);
        setContentView(binding.getRoot());
        AppointmentInsets.applySystemBars(binding.getRoot());
        appointmentId = getIntent().getLongExtra(
                EXTRA_APPOINTMENT_ID,
                -1L);
        binding.appointmentDetailToolbar.setNavigationOnClickListener(
                ignored -> finish());
        binding.appointmentDetailRetry.setOnClickListener(
                ignored -> load());
        binding.appointmentDetailEdit.setOnClickListener(
                ignored -> startActivity(
                        AppointmentFormActivity.editIntent(
                                this,
                                appointmentId)));
        binding.appointmentDetailConfirm.setOnClickListener(
                ignored -> confirm(AppointmentStatus.CONFIRMED));
        binding.appointmentDetailComplete.setOnClickListener(
                ignored -> confirm(AppointmentStatus.COMPLETED));
        binding.appointmentDetailCancel.setOnClickListener(
                ignored -> confirm(AppointmentStatus.CANCELLED));
    }

    @Override
    protected void onStart() {
        super.onStart();
        Optional<AppointmentFeatureRuntime> runtime = runtime();
        if (runtime.isEmpty() || appointmentId < 1) {
            finish();
            return;
        }
        AppointmentFeatureRuntime value = runtime.orElseThrow();
        AppointmentDetailController details =
                new AppointmentDetailController(
                        value.repository(),
                        value.workerExecutor(),
                        ContextCompat.getMainExecutor(this));
        AppointmentMutationController mutations =
                new AppointmentMutationController(
                        value.repository(),
                        value.workerExecutor(),
                        ContextCompat.getMainExecutor(this));
        detailController = Optional.of(details);
        mutationController = Optional.of(mutations);
        details.subscribe(this::renderDetail);
        mutations.subscribe(this::renderMutation);
        load();
    }

    @Override
    protected void onStop() {
        detailController.ifPresent(AppointmentDetailController::close);
        mutationController.ifPresent(
                AppointmentMutationController::close);
        detailController = Optional.empty();
        mutationController = Optional.empty();
        current = Optional.empty();
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        state.putString(STATE_KEY, attempt.key());
        state.putString(STATE_FINGERPRINT, attempt.fingerprint());
        super.onSaveInstanceState(state);
    }

    private void load() {
        runtime().ifPresent(value -> detailController.ifPresent(
                controller -> controller.load(
                        appointmentId,
                        value.scope())));
    }

    private void renderDetail(AppointmentDetailState state) {
        current = state.detail();
        renderer.renderDetail(state);
    }

    private void renderMutation(AppointmentMutationState state) {
        renderer.renderMutation(state);
        if (state.phase() == AppointmentMutationState.Phase.SUCCESS) {
            attempt.reset();
            Toast.makeText(
                    this,
                    R.string.appointment_mutation_success,
                    Toast.LENGTH_SHORT)
                    .show();
            load();
        }
    }

    private void confirm(AppointmentStatus status) {
        int title = switch (status) {
            case CONFIRMED ->
                R.string.appointment_confirm_confirm_title;
            case COMPLETED ->
                R.string.appointment_complete_confirm_title;
            case CANCELLED ->
                R.string.appointment_cancel_confirm_title;
            case SCHEDULED ->
                throw new IllegalArgumentException(
                        "Scheduled is not a transition.");
        };
        int message = switch (status) {
            case CONFIRMED ->
                R.string.appointment_confirm_confirm_message;
            case COMPLETED ->
                R.string.appointment_complete_confirm_message;
            case CANCELLED ->
                R.string.appointment_cancel_confirm_message;
            case SCHEDULED ->
                throw new IllegalArgumentException(
                        "Scheduled is not a transition.");
        };
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton(
                        R.string.appointment_action_keep,
                        null)
                .setPositiveButton(
                        R.string.appointment_action_apply,
                        (dialog, which) -> transition(status))
                .show();
    }

    private void transition(AppointmentStatus status) {
        Optional<AppointmentFeatureRuntime> runtime = runtime();
        Optional<AppointmentDetail> detail = current;
        if (runtime.isEmpty() || detail.isEmpty()) {
            return;
        }
        mutationController.ifPresent(controller ->
                controller.transition(
                        appointmentId,
                        runtime.orElseThrow().scope(),
                        detail.orElseThrow().summary().revision(),
                        status,
                        attempt.keyFor(
                                AppointmentMutationAction.TRANSITION,
                                appointmentId,
                                detail.orElseThrow()
                                        .summary()
                                        .revision(),
                                status.name())));
    }

    private Optional<AppointmentFeatureRuntime> runtime() {
        return getApplication() instanceof AppointmentRuntimeProvider provider
                ? provider.appointmentRuntime()
                : Optional.empty();
    }
}
