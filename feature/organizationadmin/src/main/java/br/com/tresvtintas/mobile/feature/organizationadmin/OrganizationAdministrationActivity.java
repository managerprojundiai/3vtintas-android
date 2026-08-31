package br.com.tresvtintas.mobile.feature.organizationadmin;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import br.com.tresvtintas.mobile.core.organizationadmin.OrganizationAdministrationException;
import br.com.tresvtintas.mobile.core.organizationadmin.OrganizationAdministrationListController;
import br.com.tresvtintas.mobile.core.organizationadmin.OrganizationAdministrationListState;
import br.com.tresvtintas.mobile.core.organizationadmin.OrganizationAdministrationModels.Mutation;
import br.com.tresvtintas.mobile.core.organizationadmin.OrganizationAdministrationModels.Organization;
import br.com.tresvtintas.mobile.core.organizationadmin.OrganizationAdministrationQuery;
import br.com.tresvtintas.mobile.core.organizationadmin.OrganizationAdministrationStatus;
import br.com.tresvtintas.mobile.feature.organizationadmin.databinding.OrganizationAdminActivityBinding;
import br.com.tresvtintas.mobile.feature.organizationadmin.databinding.OrganizationAdminCreateDialogBinding;
import br.com.tresvtintas.mobile.feature.organizationadmin.databinding.OrganizationAdminDetailDialogBinding;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

public final class OrganizationAdministrationActivity extends AppCompatActivity {
    private static final int FILTER_ACTIVE = 1;
    private static final int FILTER_BLOCKED = 2;
    private static final int FILTER_PENDING = 3;
    private static final int MINIMUM_NAME_LENGTH = 2;

    @FunctionalInterface
    private interface MutationCommand {
        Mutation execute() throws OrganizationAdministrationException;
    }

    private final AtomicBoolean mutationBusy = new AtomicBoolean();
    private final OrganizationAdministrationListController.Listener listener =
            this::render;
    private OrganizationAdminActivityBinding binding;
    private OrganizationSummaryAdapter adapter;
    private OrganizationAdministrationFeatureRuntime runtime;
    private OrganizationAdministrationListController controller;
    private Executor mainExecutor;
    private OrganizationAdministrationQuery query =
            OrganizationAdministrationQuery.initial();

    public static Intent intent(Context context) {
        return new Intent(context, OrganizationAdministrationActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OrganizationAdministrationPrivacy.protect(this);
        binding = OrganizationAdminActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mainExecutor = ContextCompat.getMainExecutor(this);

        runtime = runtimeProvider().organizationAdministrationRuntime().orElse(null);
        if (runtime == null) {
            finish();
            return;
        }
        configureViews();
        controller = new OrganizationAdministrationListController(
                runtime.repository(),
                runtime.workerExecutor(),
                mainExecutor);
        controller.subscribe(listener);
        controller.open();
    }

    @Override
    protected void onDestroy() {
        if (controller != null) {
            controller.unsubscribe(listener);
            controller.close();
        }
        super.onDestroy();
    }

    private OrganizationAdministrationRuntimeProvider runtimeProvider() {
        if (getApplication() instanceof OrganizationAdministrationRuntimeProvider provider) {
            return provider;
        }
        throw new IllegalStateException(
                "Organization administration runtime is unavailable.");
    }

    private void configureViews() {
        adapter = new OrganizationSummaryAdapter(this::showDetails);
        binding.organizations.setLayoutManager(new LinearLayoutManager(this));
        binding.organizations.setAdapter(adapter);
        binding.back.setOnClickListener(ignored -> finish());
        binding.refresh.setOnClickListener(ignored -> controller.refresh());
        binding.loadMore.setOnClickListener(ignored -> controller.loadMore());
        binding.create.setOnClickListener(ignored -> showCreate());
        binding.applyFilters.setOnClickListener(ignored -> applyFilters());
        binding.statusFilter.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                List.of(
                        getString(R.string.organization_admin_filter_all),
                        getString(R.string.organization_admin_status_active),
                        getString(R.string.organization_admin_status_blocked),
                        getString(R.string.organization_admin_status_pending))));
    }

    private void applyFilters() {
        Optional<OrganizationAdministrationStatus> status = switch (
                binding.statusFilter.getSelectedItemPosition()) {
            case FILTER_ACTIVE -> Optional.of(OrganizationAdministrationStatus.ACTIVE);
            case FILTER_BLOCKED -> Optional.of(OrganizationAdministrationStatus.BLOCKED);
            case FILTER_PENDING -> Optional.of(OrganizationAdministrationStatus.PENDING);
            default -> Optional.empty();
        };
        query = query.withStatus(status).withSearch(
                binding.search.getText().toString());
        controller.apply(query);
    }

    private void render(OrganizationAdministrationListState state) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        state.snapshot().ifPresent(snapshot -> {
            adapter.replace(snapshot.organizations());
            query = snapshot.query();
            int count = snapshot.organizations().size();
            binding.resultCount.setText(getResources().getQuantityString(
                    R.plurals.organization_admin_count,
                    count,
                    count));
            binding.empty.setVisibility(
                    snapshot.organizations().isEmpty() ? View.VISIBLE : View.GONE);
            binding.loadMore.setVisibility(
                    snapshot.nextCursor().isPresent() ? View.VISIBLE : View.GONE);
        });
        boolean loading = state.phase()
                == OrganizationAdministrationListState.Phase.LOADING;
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.applyFilters.setEnabled(!loading);
        binding.refresh.setEnabled(!loading);
        state.failure().ifPresent(failure -> Toast.makeText(
                this,
                OrganizationAdministrationText.failure(this, failure.kind()),
                Toast.LENGTH_LONG).show());
    }

    private void showCreate() {
        OrganizationAdminCreateDialogBinding dialogBinding =
                OrganizationAdminCreateDialogBinding.inflate(
                        LayoutInflater.from(this));
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.organization_admin_create_title)
                .setView(dialogBinding.getRoot())
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.organization_admin_confirm, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(
                AlertDialog.BUTTON_POSITIVE).setOnClickListener(button -> {
                    String name = dialogBinding.name.getText().toString().strip();
                    String slug = dialogBinding.slug.getText().toString().strip();
                    if (name.length() < MINIMUM_NAME_LENGTH || !validSlug(slug)) {
                        dialogBinding.validation.setVisibility(View.VISIBLE);
                        return;
                    }
                    executeMutation(
                            () -> runtime.repository().create(
                                    name,
                                    slug,
                                    UUID.randomUUID().toString()),
                            () -> {
                                dialog.dismiss();
                                controller.refresh();
                            });
                }));
        dialog.show();
    }

    private void showDetails(Organization summary) {
        runtime.workerExecutor().execute(() -> {
            try {
                Organization current = runtime.repository().organization(summary.id());
                mainExecutor.execute(() -> showDetailsDialog(current));
            } catch (OrganizationAdministrationException failure) {
                mainExecutor.execute(() -> showFailure(failure));
            }
        });
    }

    private void showDetailsDialog(Organization organization) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        OrganizationAdminDetailDialogBinding detail =
                OrganizationAdminDetailDialogBinding.inflate(
                        LayoutInflater.from(this));
        detail.name.setText(organization.name());
        detail.slug.setText(organization.slug());
        detail.status.setText(OrganizationAdministrationText.status(
                this, organization.status()));
        detail.members.setText(getString(
                R.string.organization_admin_members_format,
                organization.activeMemberCount(),
                organization.managerCount(),
                organization.salespersonCount(),
                organization.deliveryDriverCount()));
        detail.slug.setEnabled(false);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.organization_admin_detail_title)
                .setView(detail.getRoot())
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.organization_admin_save_name, null)
                .create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button -> {
                String name = detail.name.getText().toString().strip();
                if (name.length() < MINIMUM_NAME_LENGTH) {
                    detail.validation.setVisibility(View.VISIBLE);
                    return;
                }
                executeMutation(
                        () -> runtime.repository().rename(
                                organization.id(),
                                organization.revision(),
                                name,
                                UUID.randomUUID().toString()),
                        () -> {
                            dialog.dismiss();
                            controller.refresh();
                        });
            });
            detail.changeStatus.setText(organization.status()
                    == OrganizationAdministrationStatus.ACTIVE
                            ? R.string.organization_admin_block
                            : R.string.organization_admin_activate);
            detail.changeStatus.setOnClickListener(
                    button -> confirmStatusChange(organization, dialog));
        });
        dialog.show();
    }

    private void confirmStatusChange(
            Organization organization,
            AlertDialog detailDialog) {
        OrganizationAdministrationStatusConfirmation confirmation =
                OrganizationAdministrationStatusConfirmation.forOrganization(
                        organization);
        new AlertDialog.Builder(this)
                .setTitle(getString(
                        confirmation.titleResource(),
                        organization.name()))
                .setMessage(confirmation.messageResource())
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(confirmation.actionResource(), (ignored, which) ->
                        executeMutation(
                                () -> runtime.repository().setStatus(
                                        organization.id(),
                                        organization.revision(),
                                        confirmation.target(),
                                        UUID.randomUUID().toString()),
                                () -> {
                                    detailDialog.dismiss();
                                    controller.refresh();
                                }))
                .show();
    }

    private void executeMutation(MutationCommand command, Runnable success) {
        if (!mutationBusy.compareAndSet(false, true)) {
            return;
        }
        runtime.workerExecutor().execute(() -> {
            try {
                command.execute();
                mainExecutor.execute(() -> {
                    mutationBusy.set(false);
                    Toast.makeText(
                            this,
                            R.string.organization_admin_saved,
                            Toast.LENGTH_SHORT).show();
                    success.run();
                });
            } catch (OrganizationAdministrationException failure) {
                mainExecutor.execute(() -> {
                    mutationBusy.set(false);
                    showFailure(failure);
                });
            }
        });
    }

    private void showFailure(OrganizationAdministrationException failure) {
        if (!isFinishing() && !isDestroyed()) {
            Toast.makeText(
                    this,
                    OrganizationAdministrationText.failure(this, failure.kind()),
                    Toast.LENGTH_LONG).show();
        }
    }

    private static boolean validSlug(String value) {
        return value.length() >= MINIMUM_NAME_LENGTH
                && value.length() <= 120
                && value.matches("^[a-z0-9]+(?:-[a-z0-9]+)*$");
    }
}
