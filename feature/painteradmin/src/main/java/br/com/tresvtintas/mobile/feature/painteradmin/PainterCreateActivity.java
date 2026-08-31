package br.com.tresvtintas.mobile.feature.painteradmin;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationException;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.Manager;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.Mutation;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.Options;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationModels.Organization;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationTaskController;
import br.com.tresvtintas.mobile.core.painteradmin.PainterAdministrationTaskState;
import br.com.tresvtintas.mobile.core.painteradmin.PainterDraft;
import br.com.tresvtintas.mobile.feature.painteradmin.databinding.PainterAdminActivityCreateBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

public final class PainterCreateActivity extends AppCompatActivity {
    private static final int SINGLE_ORGANIZATION_COUNT = 1;
    private static final String STATE_KEY = "painter_create_key";
    private static final String STATE_FINGERPRINT = "painter_create_fingerprint";
    private PainterAdminActivityCreateBinding binding;
    private Optional<PainterAdministrationFeatureRuntime> runtime =
            Optional.empty();
    private Optional<PainterAdministrationTaskController<Options>>
            optionsController = Optional.empty();
    private Optional<PainterAdministrationTaskController<Mutation>>
            mutationController = Optional.empty();
    private Options options = new Options(List.of(), List.of());
    private List<Manager> availableManagers = List.of();
    private long organizationId;
    private OptionalLong managerUserId = OptionalLong.empty();
    private PainterAdministrationMutationAttempt attempt =
            new PainterAdministrationMutationAttempt();

    public static Intent intent(Context context) {
        return new Intent(context, PainterCreateActivity.class);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        PainterAdministrationPrivacy.protect(this);
        EdgeToEdge.enable(this);
        binding = PainterAdminActivityCreateBinding.inflate(
                getLayoutInflater());
        setContentView(binding.getRoot());
        PainterAdministrationInsets.applySystemBars(binding.getRoot());
        binding.painterCreateToolbar.setNavigationOnClickListener(
                ignored -> finish());
        binding.painterCreateSave.setOnClickListener(
                ignored -> confirmSave());
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
        if (runtime.isEmpty()) {
            showText(getString(R.string.painter_admin_error_access));
            setEnabled(false);
            return;
        }
        PainterAdministrationFeatureRuntime value = runtime.orElseThrow();
        PainterAdministrationTaskController<Options> loader =
                new PainterAdministrationTaskController<>(
                        value.workerExecutor(),
                        ContextCompat.getMainExecutor(this));
        PainterAdministrationTaskController<Mutation> saver =
                new PainterAdministrationTaskController<>(
                        value.workerExecutor(),
                        ContextCompat.getMainExecutor(this));
        optionsController = Optional.of(loader);
        mutationController = Optional.of(saver);
        loader.subscribe(this::renderOptions);
        saver.subscribe(this::renderMutation);
        loader.submit(value.repository()::options);
    }

    @Override
    protected void onStop() {
        optionsController.ifPresent(PainterAdministrationTaskController::close);
        mutationController.ifPresent(PainterAdministrationTaskController::close);
        optionsController = Optional.empty();
        mutationController = Optional.empty();
        runtime = Optional.empty();
        super.onStop();
    }

    private void renderOptions(PainterAdministrationTaskState<Options> state) {
        busy(state.phase() == PainterAdministrationTaskState.Phase.RUNNING);
        if (state.phase() == PainterAdministrationTaskState.Phase.SUCCESS) {
            options = state.result().orElseThrow();
            bindOrganizations();
            setEnabled(true);
            clearError();
        } else if (state.phase() == PainterAdministrationTaskState.Phase.ERROR) {
            showFailure(state);
            setEnabled(false);
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
            showFailure(state);
            setEnabled(true);
        }
    }

    private void bindOrganizations() {
        List<String> names = new java.util.ArrayList<>();
        for (Organization organization : options.organizations()) {
            names.add(organization.name());
        }
        binding.painterCreateOrganization.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                names));
        binding.painterCreateOrganization.setOnItemClickListener(
                (parent, view, position, id) -> {
                    organizationId =
                            options.organizations().get(position).id();
                    bindManagers();
                });
        if (names.size() == SINGLE_ORGANIZATION_COUNT) {
            organizationId = options.organizations().get(0).id();
            binding.painterCreateOrganization.setText(names.get(0), false);
            bindManagers();
        }
    }

    private void bindManagers() {
        availableManagers = options.managersFor(organizationId);
        List<String> names = new java.util.ArrayList<>();
        names.add(getString(R.string.painter_admin_no_manager));
        for (Manager manager : availableManagers) {
            names.add(manager.name());
        }
        binding.painterCreateManager.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                names));
        binding.painterCreateManager.setText(names.get(0), false);
        managerUserId = OptionalLong.empty();
        binding.painterCreateManager.setOnItemClickListener(
                (parent, view, position, id) -> managerUserId =
                        position < 1
                                ? OptionalLong.empty()
                                : OptionalLong.of(
                                        availableManagers.get(position - 1)
                                                .id()));
    }

    private void confirmSave() {
        PainterDraft draft;
        try {
            draft = draft();
        } catch (IllegalArgumentException exception) {
            binding.painterCreateNameLayout.setError(
                    getString(R.string.painter_admin_invalid_form));
            showText(getString(R.string.painter_admin_invalid_form));
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.painter_admin_create_title)
                .setMessage(R.string.painter_admin_confirm_approval)
                .setNegativeButton(R.string.painter_admin_cancel, null)
                .setPositiveButton(
                        R.string.painter_admin_confirm,
                        (dialog, ignored) -> save(draft))
                .show();
    }

    private void save(PainterDraft draft) {
        clearError();
        String fingerprint = String.join(
                "\n",
                Long.toString(draft.organizationId()),
                draft.name(),
                draft.email(),
                draft.commissionRate().toPlainString(),
                draft.managerUserId().isPresent()
                        ? Long.toString(draft.managerUserId().orElseThrow())
                        : "");
        String key = attempt.keyFor(fingerprint);
        runtime.ifPresent(value -> mutationController.ifPresent(controller ->
                controller.submit(() -> value.repository().create(draft, key))));
    }

    private PainterDraft draft() {
        return new PainterDraft(
                organizationId,
                text(binding.painterCreateName),
                text(binding.painterCreateEmail),
                optional(binding.painterCreateCpf),
                optional(binding.painterCreateRg),
                optional(binding.painterCreatePhone),
                optional(binding.painterCreateCompany),
                optional(binding.painterCreateSpecialty),
                optional(binding.painterCreateServiceArea),
                optional(binding.painterCreateAddress),
                optional(binding.painterCreateCity),
                optional(binding.painterCreateState),
                new BigDecimal(text(binding.painterCreateCommission)),
                managerUserId,
                optional(binding.painterCreateNotes));
    }

    private void busy(boolean value) {
        binding.painterCreateProgress.setVisibility(
                value ? View.VISIBLE : View.INVISIBLE);
        binding.painterCreateSave.setEnabled(!value);
    }

    private void setEnabled(boolean value) {
        binding.painterCreateSave.setEnabled(value);
        binding.painterCreateOrganization.setEnabled(value);
        binding.painterCreateManager.setEnabled(value);
    }

    private void clearError() {
        binding.painterCreateNameLayout.setError(null);
        binding.painterCreateError.setVisibility(View.GONE);
    }

    private void showFailure(PainterAdministrationTaskState<?> state) {
        PainterAdministrationException failure =
                state.failure().orElseThrow();
        String message = getString(
                PainterAdministrationText.failure(failure.kind()));
        if (failure.requestId().isPresent()) {
            message += "\n\n" + getString(
                    R.string.painter_admin_support_code,
                    failure.requestId().orElseThrow());
        }
        showText(message);
    }

    private void showText(String value) {
        binding.painterCreateError.setText(value);
        binding.painterCreateError.setVisibility(View.VISIBLE);
    }

    private Optional<PainterAdministrationFeatureRuntime> runtime() {
        return getApplication()
                        instanceof PainterAdministrationRuntimeProvider provider
                ? provider.painterAdministrationRuntime()
                : Optional.empty();
    }

    private static String text(EditText field) {
        return field.getText() == null ? "" : field.getText().toString();
    }

    private static Optional<String> optional(EditText field) {
        return Optional.of(text(field));
    }
}
