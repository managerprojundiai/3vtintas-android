package br.com.tresvtintas.mobile.feature.painteradmin;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationException;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.AccessRequestDetail;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.Manager;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.Mutation;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.Organization;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationTaskController;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationTaskState;
import br.com.tresvtintas.mobile.feature.painteradmin.databinding.PainterAdminActivityRequestBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

public final class AccessRequestDetailActivity extends AppCompatActivity {
    private static final BigDecimal MAXIMUM_COMMISSION =
            BigDecimal.valueOf(100);
    private static final int MINIMUM_REJECTION_LENGTH = 3;
    private static final int MAXIMUM_REJECTION_LENGTH = 500;
    private static final String EXTRA_ID =
            "br.com.tresvtintas.mobile.painteradmin.ACCESS_REQUEST_ID";
    private static final String STATE_KEY = "access_request_key";
    private static final String STATE_FINGERPRINT = "access_request_fingerprint";
    private PainterAdminActivityRequestBinding binding;
    private Optional<PainterAdministrationFeatureRuntime> runtime =
            Optional.empty();
    private Optional<PainterAdministrationTaskController<AccessRequestScreenData>>
            detailController = Optional.empty();
    private Optional<PainterAdministrationTaskController<Mutation>>
            mutationController = Optional.empty();
    private Optional<AccessRequestScreenData> current = Optional.empty();
    private PainterAdministrationMutationAttempt attempt =
            new PainterAdministrationMutationAttempt();
    private long requestId;

    public static Intent intent(Context context, long requestId) {
        return new Intent(context, AccessRequestDetailActivity.class)
                .putExtra(EXTRA_ID, requestId);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        PainterAdministrationPrivacy.protect(this);
        EdgeToEdge.enable(this);
        binding = PainterAdminActivityRequestBinding.inflate(
                getLayoutInflater());
        setContentView(binding.getRoot());
        PainterAdministrationInsets.applySystemBars(binding.getRoot());
        requestId = getIntent().getLongExtra(EXTRA_ID, 0);
        binding.accessRequestToolbar.setNavigationOnClickListener(
                ignored -> finish());
        binding.accessRequestRetry.setOnClickListener(ignored -> load());
        binding.accessRequestApprovePainter.setOnClickListener(
                ignored -> selectPainterOrganization());
        binding.accessRequestApproveManager.setOnClickListener(
                ignored -> selectManagerOrganization());
        binding.accessRequestReject.setOnClickListener(
                ignored -> enterRejection());
        if (state != null) {
            attempt = PainterAdministrationMutationAttempt.restored(
                    state.getString(STATE_KEY, ""),
                    state.getString(STATE_FINGERPRINT, ""));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        state.putString(STATE_KEY, attempt.key());
        state.putString(STATE_FINGERPRINT, attempt.fingerprint());
        super.onSaveInstanceState(state);
    }

    @Override
    protected void onStart() {
        super.onStart();
        runtime = runtime();
        if (runtime.isEmpty() || requestId < 1) {
            showAccessFailure();
            return;
        }
        PainterAdministrationFeatureRuntime value = runtime.orElseThrow();
        PainterAdministrationTaskController<AccessRequestScreenData> reader =
                new PainterAdministrationTaskController<>(
                        value.workerExecutor(),
                        ContextCompat.getMainExecutor(this));
        PainterAdministrationTaskController<Mutation> writer =
                new PainterAdministrationTaskController<>(
                        value.workerExecutor(),
                        ContextCompat.getMainExecutor(this));
        detailController = Optional.of(reader);
        mutationController = Optional.of(writer);
        reader.subscribe(this::renderDetail);
        writer.subscribe(this::renderMutation);
        load();
    }

    @Override
    protected void onStop() {
        detailController.ifPresent(PainterAdministrationTaskController::close);
        mutationController.ifPresent(PainterAdministrationTaskController::close);
        detailController = Optional.empty();
        mutationController = Optional.empty();
        current = Optional.empty();
        runtime = Optional.empty();
        super.onStop();
    }

    private void load() {
        runtime.ifPresent(value -> detailController.ifPresent(controller ->
                controller.submit(() -> new AccessRequestScreenData(
                        value.repository().accessRequest(requestId),
                        value.repository().options()))));
    }

    private void renderDetail(
            PainterAdministrationTaskState<AccessRequestScreenData> state) {
        busy(state.phase() == PainterAdministrationTaskState.Phase.RUNNING);
        if (state.phase() == PainterAdministrationTaskState.Phase.SUCCESS) {
            current = state.result();
            show(state.result().orElseThrow().detail());
            clearNotice();
        } else if (state.phase() == PainterAdministrationTaskState.Phase.ERROR) {
            showFailure(state.failure().orElseThrow(), true);
        }
    }

    private void renderMutation(
            PainterAdministrationTaskState<Mutation> state) {
        busy(state.phase() == PainterAdministrationTaskState.Phase.RUNNING);
        if (state.phase() == PainterAdministrationTaskState.Phase.SUCCESS) {
            attempt.reset();
            setResult(RESULT_OK);
            finish();
        } else if (state.phase() == PainterAdministrationTaskState.Phase.ERROR) {
            PainterAdministrationException failure =
                    state.failure().orElseThrow();
            showFailure(failure, false);
            if (failure.kind()
                    == br.com.tresvtintas.mobile.core.painteradmin
                            .PainterAdministrationFailureKind.CONFLICT) {
                attempt.reset();
                load();
            }
        }
    }

    private void show(AccessRequestDetail detail) {
        binding.accessRequestName.setText(detail.request().name());
        binding.accessRequestProfile.setText(
                getString(
                        R.string.painter_admin_profile_format,
                        detail.request().company().orElse("—"),
                        detail.request().specialty().orElse("—")));
        binding.accessRequestIdentity.setText(String.join(
                "\n",
                "E-mail: " + detail.request().email().orElse("—"),
                "CPF: " + detail.cpf().orElse("—"),
                "Telefone: " + detail.phone(),
                "WhatsApp: " + detail.whatsappPhone().orElse("—")));
        binding.accessRequestScroll.setVisibility(View.VISIBLE);
        binding.accessRequestErrorGroup.setVisibility(View.GONE);
    }

    private void selectPainterOrganization() {
        selectOrganization(this::selectPainterManager);
    }

    private void selectManagerOrganization() {
        selectOrganization(this::approveManager);
    }

    private void selectOrganization(
            java.util.function.LongConsumer selected) {
        if (current.isEmpty()) {
            return;
        }
        List<Organization> organizations =
                current.orElseThrow().options().organizations();
        if (organizations.isEmpty()) {
            showNotice(getString(R.string.painter_admin_error_invalid));
            return;
        }
        int[] choice = {0};
        String[] labels = organizations.stream()
                .map(Organization::name)
                .toArray(String[]::new);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.painter_admin_organization_hint)
                .setSingleChoiceItems(
                        labels,
                        choice[0],
                        (dialog, which) -> choice[0] = which)
                .setNegativeButton(R.string.painter_admin_cancel, null)
                .setPositiveButton(
                        R.string.painter_admin_confirm,
                        (dialog, ignored) -> selected.accept(
                                organizations.get(choice[0]).id()))
                .show();
    }

    private void selectPainterManager(long organizationId) {
        List<Manager> managers =
                current.orElseThrow().options().managersFor(organizationId);
        List<String> labels = new ArrayList<>();
        labels.add(getString(R.string.painter_admin_no_manager));
        for (Manager manager : managers) {
            labels.add(manager.name());
        }
        int[] choice = {0};
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.painter_admin_manager)
                .setSingleChoiceItems(
                        labels.toArray(new String[0]),
                        choice[0],
                        (dialog, which) -> choice[0] = which)
                .setNegativeButton(R.string.painter_admin_cancel, null)
                .setPositiveButton(
                        R.string.painter_admin_confirm,
                        (dialog, ignored) -> enterCommission(
                                organizationId,
                                choice[0] == 0
                                        ? OptionalLong.empty()
                                        : OptionalLong.of(
                                                managers.get(choice[0] - 1)
                                                        .id())))
                .show();
    }

    private void enterCommission(
            long organizationId,
            OptionalLong managerId) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER
                | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(R.string.painter_admin_zero_commission);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.painter_admin_commission)
                .setView(input)
                .setNegativeButton(R.string.painter_admin_cancel, null)
                .setPositiveButton(
                        R.string.painter_admin_confirm,
                        (dialog, ignored) -> approvePainter(
                                organizationId,
                                managerId,
                                input))
                .show();
    }

    private void approvePainter(
            long organizationId,
            OptionalLong managerId,
            EditText input) {
        try {
            BigDecimal rate = new BigDecimal(input.getText().toString());
            requireCommission(rate);
            int revision = current.orElseThrow()
                    .detail()
                    .request()
                    .revision();
            String manager = managerId.isPresent()
                    ? Long.toString(managerId.orElseThrow())
                    : "none";
            String key = attempt.keyFor(String.join(
                    "\n",
                    "painter",
                    Long.toString(requestId),
                    Integer.toString(revision),
                    Long.toString(organizationId),
                    manager,
                    rate.toPlainString()));
            mutationController.ifPresent(controller -> controller.submit(() ->
                    runtime.orElseThrow().repository().approvePainter(
                            requestId,
                            revision,
                            organizationId,
                            managerId,
                            rate,
                            key)));
        } catch (IllegalArgumentException exception) {
            showNotice(getString(R.string.painter_admin_invalid_form));
        }
    }

    private void approveManager(long organizationId) {
        int revision = current.orElseThrow()
                .detail()
                .request()
                .revision();
        String key = attempt.keyFor(String.join(
                "\n",
                "manager",
                Long.toString(requestId),
                Integer.toString(revision),
                Long.toString(organizationId)));
        mutationController.ifPresent(controller -> controller.submit(() ->
                runtime.orElseThrow().repository().approveManager(
                        requestId,
                        revision,
                        organizationId,
                        key)));
    }

    private void enterRejection() {
        if (current.isEmpty()) {
            return;
        }
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setHint(R.string.painter_admin_rejection_reason);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.painter_admin_reject)
                .setView(input)
                .setNegativeButton(R.string.painter_admin_cancel, null)
                .setPositiveButton(
                        R.string.painter_admin_confirm,
                        (dialog, ignored) -> reject(
                                input.getText().toString()))
                .show();
    }

    private void reject(String reason) {
        String normalized = reason == null ? "" : reason.strip();
        if (normalized.length() < MINIMUM_REJECTION_LENGTH
                || normalized.length() > MAXIMUM_REJECTION_LENGTH) {
            showNotice(getString(R.string.painter_admin_invalid_form));
            return;
        }
        int revision = current.orElseThrow()
                .detail()
                .request()
                .revision();
        String key = attempt.keyFor(String.join(
                "\n",
                "reject",
                Long.toString(requestId),
                Integer.toString(revision),
                    normalized));
        mutationController.ifPresent(controller -> controller.submit(() ->
                runtime.orElseThrow().repository().reject(
                        requestId,
                        revision,
                        normalized,
                        key)));
    }

    private static void requireCommission(BigDecimal rate) {
        if (rate.scale() > 2
                || rate.compareTo(BigDecimal.ZERO) < 0
                || rate.compareTo(MAXIMUM_COMMISSION) > 0) {
            throw new IllegalArgumentException("Commission is invalid.");
        }
    }

    private void busy(boolean value) {
        binding.accessRequestProgress.setVisibility(
                value ? View.VISIBLE : View.INVISIBLE);
        binding.accessRequestApprovePainter.setEnabled(!value);
        binding.accessRequestApproveManager.setEnabled(!value);
        binding.accessRequestReject.setEnabled(!value);
    }

    private void showAccessFailure() {
        binding.accessRequestScroll.setVisibility(View.GONE);
        binding.accessRequestErrorGroup.setVisibility(View.VISIBLE);
        binding.accessRequestError.setText(
                R.string.painter_admin_error_access);
    }

    private void showFailure(
            PainterAdministrationException failure,
            boolean terminal) {
        String message = getString(
                PainterAdministrationText.failure(failure.kind()));
        if (failure.requestId().isPresent()) {
            message += "\n\n" + getString(
                    R.string.painter_admin_support_code,
                    failure.requestId().orElseThrow());
        }
        if (terminal) {
            binding.accessRequestScroll.setVisibility(View.GONE);
            binding.accessRequestErrorGroup.setVisibility(View.VISIBLE);
            binding.accessRequestError.setText(message);
        } else {
            showNotice(message);
        }
    }

    private void clearNotice() {
        binding.accessRequestNotice.setVisibility(View.GONE);
    }

    private void showNotice(String value) {
        binding.accessRequestNotice.setText(value);
        binding.accessRequestNotice.setVisibility(View.VISIBLE);
    }

    private Optional<PainterAdministrationFeatureRuntime> runtime() {
        return getApplication()
                        instanceof PainterAdministrationRuntimeProvider provider
                ? provider.painterAdministrationRuntime()
                : Optional.empty();
    }
}
