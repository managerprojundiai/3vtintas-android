package br.com.tresvtintas.mobile.feature.useradmin;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import br.com.tresvtintas.mobile.core.model.AppRole;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationException;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationFailureKind;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationModels.Assignment;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationModels.Mutation;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationModels.Options;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationModels.Organization;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationModels.User;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationTaskController;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationTaskState;
import br.com.tresvtintas.mobile.feature.useradmin.databinding.UserAdminActivityDetailBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.List;
import java.util.Optional;

public final class UserAdministrationDetailActivity extends AppCompatActivity {
    private static final String EXTRA_ID =
            "br.com.tresvtintas.mobile.useradmin.USER_ID";
    private static final String STATE_KEY = "user_admin_key";
    private static final String STATE_FINGERPRINT = "user_admin_fingerprint";
    private UserAdminActivityDetailBinding binding;
    private Optional<UserAdministrationFeatureRuntime> runtime = Optional.empty();
    private Optional<UserAdministrationTaskController<ScreenData>> reader = Optional.empty();
    private Optional<UserAdministrationTaskController<Mutation>> writer = Optional.empty();
    private Optional<ScreenData> current = Optional.empty();
    private UserAdministrationMutationAttempt attempt =
            new UserAdministrationMutationAttempt();
    private long userId;

    public static Intent intent(Context context, long userId) {
        return new Intent(context, UserAdministrationDetailActivity.class)
                .putExtra(EXTRA_ID, userId);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        UserAdministrationPrivacy.protect(this);
        EdgeToEdge.enable(this);
        binding = UserAdminActivityDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UserAdministrationInsets.applySystemBars(binding.getRoot());
        userId = getIntent().getLongExtra(EXTRA_ID, 0);
        binding.userAdminDetailToolbar.setNavigationOnClickListener(ignored -> finish());
        binding.userAdminDetailRetry.setOnClickListener(ignored -> load());
        binding.userAdminDetailStandardRole.setOnClickListener(
                ignored -> chooseStandardRole());
        binding.userAdminDetailOperationalRole.setOnClickListener(
                ignored -> chooseOperationalRole());
        binding.userAdminDetailStatus.setOnClickListener(
                ignored -> confirmStatusChange());
        binding.userAdminDetailSecurityAccess.setOnClickListener(
                ignored -> openSecurityAccess());
        if (state != null) {
            attempt = UserAdministrationMutationAttempt.restored(
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
        if (runtime.isEmpty() || userId < 1) {
            showAccessFailure();
            return;
        }
        UserAdministrationFeatureRuntime value = runtime.orElseThrow();
        UserAdministrationTaskController<ScreenData> nextReader =
                new UserAdministrationTaskController<>(
                        value.workerExecutor(),
                        ContextCompat.getMainExecutor(this));
        UserAdministrationTaskController<Mutation> nextWriter =
                new UserAdministrationTaskController<>(
                        value.workerExecutor(),
                        ContextCompat.getMainExecutor(this));
        reader = Optional.of(nextReader);
        writer = Optional.of(nextWriter);
        nextReader.subscribe(this::renderRead);
        nextWriter.subscribe(this::renderMutation);
        load();
    }

    @Override
    protected void onStop() {
        reader.ifPresent(UserAdministrationTaskController::close);
        writer.ifPresent(UserAdministrationTaskController::close);
        reader = Optional.empty();
        writer = Optional.empty();
        runtime = Optional.empty();
        current = Optional.empty();
        super.onStop();
    }

    private void load() {
        runtime.ifPresent(value -> reader.ifPresent(controller -> controller.submit(
                () -> new ScreenData(
                        value.repository().user(userId),
                        value.repository().options()))));
    }

    private void renderRead(UserAdministrationTaskState<ScreenData> state) {
        busy(state.phase() == UserAdministrationTaskState.Phase.RUNNING);
        if (state.phase() == UserAdministrationTaskState.Phase.SUCCESS) {
            current = state.result();
            show(state.result().orElseThrow().user());
            clearNotice();
        } else if (state.phase() == UserAdministrationTaskState.Phase.ERROR) {
            current = Optional.empty();
            showFailure(state.failure().orElseThrow(), true);
        }
    }

    private void renderMutation(UserAdministrationTaskState<Mutation> state) {
        busy(state.phase() == UserAdministrationTaskState.Phase.RUNNING);
        if (state.phase() == UserAdministrationTaskState.Phase.SUCCESS) {
            Mutation mutation = state.result().orElseThrow();
            attempt.reset();
            showNotice(getString(
                    mutation.replayed()
                            ? R.string.user_admin_replayed
                            : R.string.user_admin_success));
            load();
        } else if (state.phase() == UserAdministrationTaskState.Phase.ERROR) {
            UserAdministrationException failure = state.failure().orElseThrow();
            showFailure(failure, false);
            if (failure.kind() == UserAdministrationFailureKind.CONFLICT) {
                attempt.reset();
                load();
            }
        }
    }

    private void show(User user) {
        binding.userAdminDetailName.setText(user.name());
        binding.userAdminDetailEmail.setText(user.email().orElse(
                getString(R.string.user_admin_no_email)));
        binding.userAdminDetailRole.setText(getString(
                R.string.user_admin_role_status,
                getString(UserAdministrationText.role(user.role())),
                getString(user.blocked()
                        ? R.string.user_admin_status_blocked
                        : R.string.user_admin_status_active)));
        binding.userAdminDetailAssignments.setText(assignments(user.assignments()));
        boolean protectedTarget = user.protectedFromChanges(
                runtime.orElseThrow().actorUserId());
        binding.userAdminDetailProtection.setVisibility(
                protectedTarget ? View.VISIBLE : View.GONE);
        binding.userAdminDetailStandardRole.setEnabled(!protectedTarget);
        binding.userAdminDetailOperationalRole.setEnabled(!protectedTarget);
        binding.userAdminDetailStatus.setEnabled(!protectedTarget);
        binding.userAdminDetailStatus.setText(user.blocked()
                ? R.string.user_admin_unblock
                : R.string.user_admin_block);
        boolean securityAvailable = runtime.orElseThrow()
                .securityNavigator()
                .isPresent();
        binding.userAdminDetailSecurityAccess.setVisibility(
                securityAvailable ? View.VISIBLE : View.GONE);
        binding.userAdminDetailSecurityAccess.setEnabled(
                securityAvailable);
        binding.userAdminDetailScroll.setVisibility(View.VISIBLE);
        binding.userAdminDetailErrorGroup.setVisibility(View.GONE);
    }

    private String assignments(List<Assignment> values) {
        if (values.isEmpty()) {
            return getString(R.string.user_admin_no_assignment);
        }
        StringBuilder result = new StringBuilder();
        for (Assignment assignment : values) {
            if (result.length() > 0) {
                result.append('\n');
            }
            result.append(assignment.organizationName())
                    .append(" • ")
                    .append(getString(UserAdministrationText.role(assignment.role())));
        }
        return result.toString();
    }

    private void chooseStandardRole() {
        if (!editable()) {
            return;
        }
        List<AppRole> roles = current.orElseThrow().options().standardRoles();
        String[] labels = roles.stream()
                .map(role -> getString(UserAdministrationText.role(role)))
                .toArray(String[]::new);
        int[] selected = {0};
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.user_admin_standard_role)
                .setSingleChoiceItems(labels, 0, (dialog, which) -> selected[0] = which)
                .setNegativeButton(R.string.user_admin_cancel, null)
                .setPositiveButton(
                        R.string.user_admin_continue,
                        (dialog, ignored) -> confirmStandardRole(roles.get(selected[0])))
                .show();
    }

    private void confirmStandardRole(AppRole role) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.user_admin_confirm_title)
                .setMessage(getString(
                        R.string.user_admin_confirm_standard,
                        getString(UserAdministrationText.role(role))))
                .setNegativeButton(R.string.user_admin_cancel, null)
                .setPositiveButton(
                        R.string.user_admin_confirm,
                        (dialog, ignored) -> submitStandardRole(role))
                .show();
    }

    private void submitStandardRole(AppRole role) {
        User user = current.orElseThrow().user();
        String key = attempt.keyFor("standard_role\n"
                + user.id() + "\n" + user.revision() + "\n" + role.wireValue());
        writer.ifPresent(controller -> controller.submit(() ->
                runtime.orElseThrow().repository().assignStandardRole(
                        user.id(),
                        user.revision(),
                        role,
                        key)));
    }

    private void chooseOperationalRole() {
        if (!editable()) {
            return;
        }
        List<AppRole> roles = current.orElseThrow().options().operationalRoles();
        String[] labels = roles.stream()
                .map(role -> getString(UserAdministrationText.role(role)))
                .toArray(String[]::new);
        int[] selected = {0};
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.user_admin_operational_role)
                .setSingleChoiceItems(labels, 0, (dialog, which) -> selected[0] = which)
                .setNegativeButton(R.string.user_admin_cancel, null)
                .setPositiveButton(
                        R.string.user_admin_continue,
                        (dialog, ignored) -> chooseOrganization(roles.get(selected[0])))
                .show();
    }

    private void chooseOrganization(AppRole role) {
        List<Organization> organizations = current.orElseThrow().options().organizations();
        String[] labels = organizations.stream().map(Organization::name).toArray(String[]::new);
        int[] selected = {0};
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.user_admin_choose_organization)
                .setSingleChoiceItems(labels, 0, (dialog, which) -> selected[0] = which)
                .setNegativeButton(R.string.user_admin_cancel, null)
                .setPositiveButton(
                        R.string.user_admin_continue,
                        (dialog, ignored) -> confirmOperationalRole(
                                role,
                                organizations.get(selected[0])))
                .show();
    }

    private void confirmOperationalRole(AppRole role, Organization organization) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.user_admin_confirm_title)
                .setMessage(getString(
                        R.string.user_admin_confirm_operational,
                        getString(UserAdministrationText.role(role)),
                        organization.name()))
                .setNegativeButton(R.string.user_admin_cancel, null)
                .setPositiveButton(
                        R.string.user_admin_confirm,
                        (dialog, ignored) -> submitOperationalRole(role, organization))
                .show();
    }

    private void submitOperationalRole(AppRole role, Organization organization) {
        User user = current.orElseThrow().user();
        String key = attempt.keyFor("operational_role\n"
                + user.id() + "\n" + user.revision() + "\n"
                + role.wireValue() + "\n" + organization.id());
        writer.ifPresent(controller -> controller.submit(() ->
                runtime.orElseThrow().repository().assignOperationalRole(
                        user.id(),
                        user.revision(),
                        role,
                        organization.id(),
                        key)));
    }

    private void confirmStatusChange() {
        if (!editable()) {
            return;
        }
        User user = current.orElseThrow().user();
        boolean block = !user.blocked();
        new MaterialAlertDialogBuilder(this)
                .setTitle(block ? R.string.user_admin_block : R.string.user_admin_unblock)
                .setMessage(block
                        ? R.string.user_admin_confirm_block
                        : R.string.user_admin_confirm_unblock)
                .setNegativeButton(R.string.user_admin_cancel, null)
                .setPositiveButton(
                        R.string.user_admin_confirm,
                        (dialog, ignored) -> submitStatus(block))
                .show();
    }

    private void openSecurityAccess() {
        current.map(ScreenData::user).ifPresent(user ->
                runtime.flatMap(
                                UserAdministrationFeatureRuntime::securityNavigator)
                        .ifPresent(navigator -> navigator.open(
                                this,
                                user.id(),
                                user.name())));
    }

    private void submitStatus(boolean blocked) {
        User user = current.orElseThrow().user();
        String key = attempt.keyFor("account_status\n"
                + user.id() + "\n" + user.revision() + "\n" + blocked);
        writer.ifPresent(controller -> controller.submit(() ->
                runtime.orElseThrow().repository().setBlocked(
                        user.id(),
                        user.revision(),
                        blocked,
                        key)));
    }

    private boolean editable() {
        return current.map(ScreenData::user)
                .filter(user -> !user.protectedFromChanges(
                        runtime.orElseThrow().actorUserId()))
                .isPresent();
    }

    private void busy(boolean value) {
        binding.userAdminDetailProgress.setVisibility(
                value ? View.VISIBLE : View.INVISIBLE);
        boolean editable = !value && editable();
        binding.userAdminDetailStandardRole.setEnabled(editable);
        binding.userAdminDetailOperationalRole.setEnabled(editable);
        binding.userAdminDetailStatus.setEnabled(editable);
        binding.userAdminDetailSecurityAccess.setEnabled(
                !value
                        && current.isPresent()
                        && runtime.flatMap(
                                        UserAdministrationFeatureRuntime::securityNavigator)
                                .isPresent());
    }

    private void showAccessFailure() {
        binding.userAdminDetailScroll.setVisibility(View.GONE);
        binding.userAdminDetailErrorGroup.setVisibility(View.VISIBLE);
        binding.userAdminDetailError.setText(R.string.user_admin_error_access);
    }

    private void showFailure(UserAdministrationException failure, boolean terminal) {
        String message = getString(UserAdministrationText.failure(failure.kind()));
        if (failure.requestId().isPresent()) {
            message += "\n\n" + getString(
                    R.string.user_admin_support_code,
                    failure.requestId().orElseThrow());
        }
        if (terminal) {
            binding.userAdminDetailScroll.setVisibility(View.GONE);
            binding.userAdminDetailErrorGroup.setVisibility(View.VISIBLE);
            binding.userAdminDetailError.setText(message);
        } else {
            showNotice(message);
        }
    }

    private void clearNotice() {
        binding.userAdminDetailNotice.setVisibility(View.GONE);
    }

    private void showNotice(String value) {
        binding.userAdminDetailNotice.setText(value);
        binding.userAdminDetailNotice.setVisibility(View.VISIBLE);
    }

    private Optional<UserAdministrationFeatureRuntime> runtime() {
        return getApplication() instanceof UserAdministrationRuntimeProvider provider
                ? provider.userAdministrationRuntime()
                : Optional.empty();
    }

    private record ScreenData(User user, Options options) {
        private ScreenData {
            if (user == null || options == null) {
                throw new IllegalArgumentException("User screen data are invalid.");
            }
        }
    }
}
