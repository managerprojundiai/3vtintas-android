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
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.Manager;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.Mutation;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.Painter;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.PainterDetail;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationStatus;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationTaskController;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationTaskState;
import br.com.tresvtintas.mobile.feature.painteradmin.databinding.PainterAdminActivityDetailBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

public final class PainterDetailActivity extends AppCompatActivity {
    private static final BigDecimal MAXIMUM_COMMISSION =
            BigDecimal.valueOf(100);
    private static final String EXTRA_ID =
            "br.com.tresvtintas.mobile.painteradmin.PAINTER_ID";
    private static final String STATE_KEY = "painter_detail_key";
    private static final String STATE_FINGERPRINT = "painter_detail_fingerprint";
    private PainterAdminActivityDetailBinding binding;
    private Optional<PainterAdministrationFeatureRuntime> runtime =
            Optional.empty();
    private Optional<PainterAdministrationTaskController<PainterDetailScreenData>>
            detailController = Optional.empty();
    private Optional<PainterAdministrationTaskController<Mutation>>
            mutationController = Optional.empty();
    private Optional<PainterDetailScreenData> current = Optional.empty();
    private PainterAdministrationMutationAttempt attempt =
            new PainterAdministrationMutationAttempt();
    private long painterId;

    public static Intent intent(Context context, long painterId) {
        return new Intent(context, PainterDetailActivity.class)
                .putExtra(EXTRA_ID, painterId);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        PainterAdministrationPrivacy.protect(this);
        EdgeToEdge.enable(this);
        binding = PainterAdminActivityDetailBinding.inflate(
                getLayoutInflater());
        setContentView(binding.getRoot());
        PainterAdministrationInsets.applySystemBars(binding.getRoot());
        painterId = getIntent().getLongExtra(EXTRA_ID, 0);
        binding.painterDetailToolbar.setNavigationOnClickListener(
                ignored -> finish());
        binding.painterDetailRetry.setOnClickListener(ignored -> load());
        binding.painterDetailStatus.setOnClickListener(
                ignored -> chooseStatus());
        binding.painterDetailCommission.setOnClickListener(
                ignored -> changeCommission());
        binding.painterDetailManager.setOnClickListener(
                ignored -> chooseManager());
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
        if (runtime.isEmpty() || painterId < 1) {
            showAccessFailure();
            return;
        }
        PainterAdministrationFeatureRuntime value = runtime.orElseThrow();
        PainterAdministrationTaskController<PainterDetailScreenData> reader =
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
                controller.submit(() -> new PainterDetailScreenData(
                        value.repository().painter(painterId),
                        value.repository().options()))));
    }

    private void renderDetail(
            PainterAdministrationTaskState<PainterDetailScreenData> state) {
        busy(state.phase() == PainterAdministrationTaskState.Phase.RUNNING);
        if (state.phase() == PainterAdministrationTaskState.Phase.SUCCESS) {
            current = state.result();
            show(state.result().orElseThrow().detail());
            clearNotice();
        } else if (state.phase() == PainterAdministrationTaskState.Phase.ERROR) {
            current = Optional.empty();
            showFailure(state.failure().orElseThrow(), true);
        }
    }

    private void renderMutation(
            PainterAdministrationTaskState<Mutation> state) {
        busy(state.phase() == PainterAdministrationTaskState.Phase.RUNNING);
        if (state.phase() == PainterAdministrationTaskState.Phase.SUCCESS) {
            Mutation mutation = state.result().orElseThrow();
            attempt.reset();
            showNotice(getString(
                    mutation.replayed()
                            ? R.string.painter_admin_replayed
                            : R.string.painter_admin_success));
            load();
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

    private void show(PainterDetail detail) {
        Painter painter = detail.painter();
        binding.painterDetailName.setText(painter.name());
        binding.painterDetailAssignment.setText(
                getString(
                        R.string.painter_admin_assignment_format,
                        painter.organization().name(),
                        painter.manager().map(Manager::name)
                                .orElse(getString(
                                        R.string.painter_admin_no_manager)),
                        painter.commissionRate().toPlainString(),
                        getString(PainterAdministrationText.status(
                                painter.status()))));
        binding.painterDetailIdentity.setText(lines(
                "E-mail", painter.email().orElse("—"),
                "CPF", detail.cpf().orElse("—"),
                "RG", detail.rg().orElse("—")));
        binding.painterDetailContact.setText(lines(
                "Telefone", detail.phone().orElse("—"),
                "WhatsApp", detail.whatsappPhone().orElse("—"),
                "Endereço", detail.address().orElse("—"),
                "Cidade/UF", detail.city().orElse("—")
                        + "/"
                        + detail.state().orElse("—")));
        binding.painterDetailWork.setText(lines(
                "Empresa", painter.company().orElse("—"),
                "Especialidade", painter.specialty().orElse("—"),
                "Área de atendimento", detail.serviceArea().orElse("—"),
                "Observações", detail.notes().orElse("—")));
        binding.painterDetailScroll.setVisibility(View.VISIBLE);
        binding.painterDetailErrorGroup.setVisibility(View.GONE);
    }

    private void chooseStatus() {
        if (current.isEmpty()) {
            return;
        }
        PainterAdministrationStatus[] values =
                PainterAdministrationStatus.values();
        String[] labels = {
            getString(R.string.painter_admin_status_pending),
            getString(R.string.painter_admin_status_active),
            getString(R.string.painter_admin_status_blocked)
        };
        int[] selected = {current.orElseThrow()
                .detail()
                .painter()
                .status()
                .ordinal()};
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.painter_admin_change_status)
                .setSingleChoiceItems(
                        labels,
                        selected[0],
                        (dialog, which) -> selected[0] = which)
                .setNegativeButton(R.string.painter_admin_cancel, null)
                .setPositiveButton(
                        R.string.painter_admin_confirm,
                        (dialog, ignored) -> updateStatus(values[selected[0]]))
                .show();
    }

    private void updateStatus(PainterAdministrationStatus status) {
        PainterDetail detail = current.orElseThrow().detail();
        String fingerprint = "status\n"
                + painterId
                + "\n"
                + detail.painter().revision()
                + "\n"
                + status.wireValue();
        String key = attempt.keyFor(fingerprint);
        mutationController.ifPresent(controller -> controller.submit(() ->
                runtime.orElseThrow().repository().updateStatus(
                        painterId,
                        detail.painter().revision(),
                        status,
                        key)));
    }

    private void changeCommission() {
        if (current.isEmpty()) {
            return;
        }
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER
                | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(current.orElseThrow()
                .detail()
                .painter()
                .commissionRate()
                .toPlainString());
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.painter_admin_change_commission)
                .setView(input)
                .setNegativeButton(R.string.painter_admin_cancel, null)
                .setPositiveButton(
                        R.string.painter_admin_confirm,
                        (dialog, ignored) -> updateCommission(input))
                .show();
    }

    private void updateCommission(EditText input) {
        try {
            BigDecimal rate = new BigDecimal(input.getText().toString());
            requireCommission(rate);
            PainterDetail detail = current.orElseThrow().detail();
            String fingerprint = "commission\n"
                    + painterId
                    + "\n"
                    + detail.painter().revision()
                    + "\n"
                    + rate.toPlainString();
            String key = attempt.keyFor(fingerprint);
            mutationController.ifPresent(controller -> controller.submit(() ->
                    runtime.orElseThrow().repository().updateCommission(
                            painterId,
                            detail.painter().revision(),
                            rate,
                            key)));
        } catch (IllegalArgumentException exception) {
            showNotice(getString(R.string.painter_admin_invalid_form));
        }
    }

    private static void requireCommission(BigDecimal rate) {
        if (rate.scale() > 2
                || rate.compareTo(BigDecimal.ZERO) < 0
                || rate.compareTo(MAXIMUM_COMMISSION) > 0) {
            throw new IllegalArgumentException("Commission is invalid.");
        }
    }

    private void chooseManager() {
        if (current.isEmpty()) {
            return;
        }
        PainterDetailScreenData data = current.orElseThrow();
        long organizationId = data.detail().painter().organization().id();
        List<Manager> managers =
                data.options().managersFor(organizationId);
        List<String> labels = new ArrayList<>();
        labels.add(getString(R.string.painter_admin_no_manager));
        for (Manager manager : managers) {
            labels.add(manager.name());
        }
        int[] selected = {0};
        data.detail().painter().manager().ifPresent(currentManager -> {
            for (int index = 0; index < managers.size(); index++) {
                if (managers.get(index).id() == currentManager.id()) {
                    selected[0] = index + 1;
                }
            }
        });
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.painter_admin_change_manager)
                .setSingleChoiceItems(
                        labels.toArray(new String[0]),
                        selected[0],
                        (dialog, which) -> selected[0] = which)
                .setNegativeButton(R.string.painter_admin_cancel, null)
                .setPositiveButton(
                        R.string.painter_admin_confirm,
                        (dialog, ignored) -> updateManager(
                                selected[0] == 0
                                        ? OptionalLong.empty()
                                        : OptionalLong.of(
                                                managers.get(selected[0] - 1)
                                                        .id())))
                .show();
    }

    private void updateManager(OptionalLong managerId) {
        PainterDetail detail = current.orElseThrow().detail();
        String manager = managerId.isPresent()
                ? Long.toString(managerId.orElseThrow())
                : "none";
        String key = attempt.keyFor(
                "manager\n"
                        + painterId
                        + "\n"
                        + detail.painter().revision()
                        + "\n"
                        + manager);
        mutationController.ifPresent(controller -> controller.submit(() ->
                runtime.orElseThrow().repository().updateManager(
                        painterId,
                        detail.painter().revision(),
                        managerId,
                        key)));
    }

    private void busy(boolean value) {
        binding.painterDetailProgress.setVisibility(
                value ? View.VISIBLE : View.INVISIBLE);
        binding.painterDetailStatus.setEnabled(!value);
        binding.painterDetailCommission.setEnabled(!value);
        binding.painterDetailManager.setEnabled(!value);
    }

    private void showAccessFailure() {
        binding.painterDetailScroll.setVisibility(View.GONE);
        binding.painterDetailErrorGroup.setVisibility(View.VISIBLE);
        binding.painterDetailError.setText(
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
            binding.painterDetailScroll.setVisibility(View.GONE);
            binding.painterDetailErrorGroup.setVisibility(View.VISIBLE);
            binding.painterDetailError.setText(message);
        } else {
            showNotice(message);
        }
    }

    private void clearNotice() {
        binding.painterDetailNotice.setVisibility(View.GONE);
    }

    private void showNotice(String value) {
        binding.painterDetailNotice.setText(value);
        binding.painterDetailNotice.setVisibility(View.VISIBLE);
    }

    private Optional<PainterAdministrationFeatureRuntime> runtime() {
        return getApplication()
                        instanceof PainterAdministrationRuntimeProvider provider
                ? provider.painterAdministrationRuntime()
                : Optional.empty();
    }

    private static String lines(String... values) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < values.length; index += 2) {
            if (result.length() > 0) {
                result.append('\n');
            }
            result.append(values[index]).append(": ").append(values[index + 1]);
        }
        return result.toString();
    }
}
