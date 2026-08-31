package br.com.tresvtintas.mobile.feature.catalogadmin;

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
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationException;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationListController;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationListState;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationModels.Category;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationModels.Product;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationModels.ProductDraft;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogAdministrationQuery;
import br.com.tresvtintas.mobile.feature.catalogadmin.databinding.CatalogAdminActivityBinding;
import br.com.tresvtintas.mobile.feature.catalogadmin.databinding.CatalogAdminCategoryDialogBinding;
import br.com.tresvtintas.mobile.feature.catalogadmin.databinding.CatalogAdminKnowledgeDialogBinding;
import br.com.tresvtintas.mobile.feature.catalogadmin.databinding.CatalogAdminProductDialogBinding;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CatalogAdministrationActivity extends AppCompatActivity {
    private static final int FILTER_ACTIVE = 1;
    private static final int FILTER_INACTIVE = 2;
    private static final int MINIMUM_CATEGORY_NAME = 2;

    @FunctionalInterface
    private interface MutationCommand {
        void execute() throws CatalogAdministrationException;
    }

    private final AtomicBoolean mutationBusy = new AtomicBoolean();
    private final CatalogAdministrationListController.Listener listener =
            this::render;
    private CatalogAdminActivityBinding binding;
    private CatalogAdministrationProductAdapter adapter;
    private CatalogAdministrationFeatureRuntime runtime;
    private CatalogAdministrationListController controller;
    private Executor mainExecutor;
    private CatalogAdministrationQuery query =
            CatalogAdministrationQuery.initial();

    public static Intent intent(Context context) {
        return new Intent(context, CatalogAdministrationActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CatalogAdministrationPrivacy.protect(this);
        binding = CatalogAdminActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mainExecutor = ContextCompat.getMainExecutor(this);
        runtime = runtimeProvider().catalogAdministrationRuntime().orElse(null);
        if (runtime == null) {
            finish();
            return;
        }
        configureViews();
        controller = new CatalogAdministrationListController(
                runtime.repository(),
                runtime.workerExecutor(),
                mainExecutor);
        controller.subscribe(listener);
        controller.open(query);
    }

    @Override
    protected void onDestroy() {
        if (controller != null) {
            controller.unsubscribe(listener);
            controller.close();
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (controller != null) {
            controller.refresh();
        }
    }

    private CatalogAdministrationRuntimeProvider runtimeProvider() {
        if (getApplication()
                instanceof CatalogAdministrationRuntimeProvider provider) {
            return provider;
        }
        throw new IllegalStateException(
                "Catalog administration runtime is unavailable.");
    }

    private void configureViews() {
        adapter = new CatalogAdministrationProductAdapter(this::openProduct);
        binding.products.setLayoutManager(new LinearLayoutManager(this));
        binding.products.setAdapter(adapter);
        binding.back.setOnClickListener(ignored -> finish());
        binding.refresh.setOnClickListener(ignored -> controller.refresh());
        binding.loadMore.setOnClickListener(ignored -> controller.loadMore());
        binding.createProduct.setOnClickListener(
                ignored -> loadProductDialog(Optional.empty()));
        binding.createCategory.setOnClickListener(
                ignored -> showCategoryDialog());
        binding.importProducts.setOnClickListener(
                ignored -> startActivity(CatalogImportActivity.intent(this)));
        binding.applyFilters.setOnClickListener(ignored -> applyFilters());
        binding.statusFilter.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                List.of(
                        getString(R.string.catalog_admin_filter_all),
                        getString(R.string.catalog_admin_filter_active),
                        getString(R.string.catalog_admin_filter_inactive))));
    }

    private void applyFilters() {
        Optional<Boolean> active = switch (
                binding.statusFilter.getSelectedItemPosition()) {
            case FILTER_ACTIVE -> Optional.of(true);
            case FILTER_INACTIVE -> Optional.of(false);
            default -> Optional.empty();
        };
        query = query.withActive(active).withSearch(
                binding.search.getText().toString());
        controller.open(query);
    }

    private void render(CatalogAdministrationListState state) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        adapter.replace(state.products());
        int count = state.products().size();
        binding.resultCount.setText(getResources().getQuantityString(
                R.plurals.catalog_admin_count,
                count,
                count));
        binding.empty.setVisibility(
                state.phase() == CatalogAdministrationListState.Phase.READY
                        && state.products().isEmpty()
                        ? View.VISIBLE
                        : View.GONE);
        binding.loadMore.setVisibility(
                state.nextCursor().isPresent() ? View.VISIBLE : View.GONE);
        boolean loading =
                state.phase() == CatalogAdministrationListState.Phase.LOADING;
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.applyFilters.setEnabled(!loading);
        binding.refresh.setEnabled(!loading);
        binding.createProduct.setEnabled(!loading);
        state.failure().ifPresent(this::showFailure);
    }

    private void openProduct(Product summary) {
        loadProductDialog(Optional.of(summary.id()));
    }

    private void loadProductDialog(Optional<Long> productId) {
        if (!mutationBusy.compareAndSet(false, true)) {
            return;
        }
        runtime.workerExecutor().execute(() -> {
            try {
                List<Category> categories = runtime.repository().categories();
                Product product = productId.isPresent()
                        ? runtime.repository().product(productId.orElseThrow())
                        : null;
                mainExecutor.execute(() -> {
                    mutationBusy.set(false);
                    showProductDialog(product, categories);
                });
            } catch (CatalogAdministrationException failure) {
                mainExecutor.execute(() -> {
                    mutationBusy.set(false);
                    showFailure(failure);
                });
            }
        });
    }

    private void showProductDialog(
            Product product,
            List<Category> categories) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        CatalogAdminProductDialogBinding form =
                CatalogAdminProductDialogBinding.inflate(
                        LayoutInflater.from(this));
        CatalogAdministrationForm.configureProduct(
                this,
                form,
                categories,
                product);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(product == null
                        ? R.string.catalog_admin_product_create_title
                        : R.string.catalog_admin_product_edit_title)
                .setView(form.getRoot())
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.catalog_admin_save, null)
                .create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(
                    button -> saveProduct(dialog, form, categories, product));
            if (product != null) {
                form.changeStatus.setOnClickListener(
                        button -> confirmStatus(product, dialog));
                form.knowledge.setOnClickListener(
                        button -> showKnowledgeDialog(product, dialog));
            }
        });
        dialog.show();
        CatalogAdministrationPrivacy.protect(dialog);
    }

    private void saveProduct(
            AlertDialog dialog,
            CatalogAdminProductDialogBinding form,
            List<Category> categories,
            Product product) {
        Optional<ProductDraft> draft =
                CatalogAdministrationForm.productDraft(form, categories);
        if (draft.isEmpty()) {
            form.validation.setVisibility(View.VISIBLE);
            return;
        }
        String key = UUID.randomUUID().toString();
        executeMutation(
                product == null
                        ? () -> runtime.repository().create(
                                draft.orElseThrow(),
                                key)
                        : () -> runtime.repository().update(
                                product.id(),
                                product.revision(),
                                draft.orElseThrow(),
                                key),
                () -> {
                    dialog.dismiss();
                    controller.refresh();
                });
    }

    private void confirmStatus(Product product, AlertDialog detailDialog) {
        boolean target = !product.active();
        String targetLabel = getString(target
                ? R.string.catalog_admin_active
                : R.string.catalog_admin_inactive)
                .toLowerCase(Locale.getDefault());
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.catalog_admin_status_confirmation_title)
                .setMessage(getString(
                        R.string.catalog_admin_status_confirmation_message,
                        targetLabel))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (ignored, which) ->
                        executeMutation(
                                () -> runtime.repository().setActive(
                                        product.id(),
                                        product.revision(),
                                        target,
                                        UUID.randomUUID().toString()),
                                () -> {
                                    detailDialog.dismiss();
                                    controller.refresh();
                                }))
                .create();
        dialog.show();
        CatalogAdministrationPrivacy.protect(dialog);
    }

    private void showKnowledgeDialog(
            Product product,
            AlertDialog productDialog) {
        CatalogAdminKnowledgeDialogBinding knowledge =
                CatalogAdminKnowledgeDialogBinding.inflate(
                        LayoutInflater.from(this));
        CatalogAdministrationForm.configureKnowledge(
                knowledge,
                product.knowledge());
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.catalog_admin_knowledge_title)
                .setView(knowledge.getRoot())
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(
                        R.string.catalog_admin_save_knowledge,
                        null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(
                AlertDialog.BUTTON_POSITIVE).setOnClickListener(button ->
                        executeMutation(
                                () -> runtime.repository().updateKnowledge(
                                        product.id(),
                                        product.revision(),
                                        CatalogAdministrationForm
                                                .knowledgeDraft(knowledge),
                                        UUID.randomUUID().toString()),
                                () -> {
                                    dialog.dismiss();
                                    productDialog.dismiss();
                                    controller.refresh();
                                })));
        dialog.show();
        CatalogAdministrationPrivacy.protect(dialog);
    }

    private void showCategoryDialog() {
        CatalogAdminCategoryDialogBinding category =
                CatalogAdminCategoryDialogBinding.inflate(
                        LayoutInflater.from(this));
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.catalog_admin_category_title)
                .setView(category.getRoot())
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(
                        R.string.catalog_admin_create_category_action,
                        null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(
                AlertDialog.BUTTON_POSITIVE).setOnClickListener(button -> {
                    String name = category.name.getText().toString().strip();
                    String description =
                            category.description.getText().toString().strip();
                    if (name.length() < MINIMUM_CATEGORY_NAME) {
                        category.validation.setVisibility(View.VISIBLE);
                        return;
                    }
                    executeMutation(
                            () -> runtime.repository().createCategory(
                                    name,
                                    Optional.of(description)
                                            .filter(value -> !value.isEmpty()),
                                    UUID.randomUUID().toString()),
                            dialog::dismiss);
                }));
        dialog.show();
        CatalogAdministrationPrivacy.protect(dialog);
    }

    private void executeMutation(
            MutationCommand command,
            Runnable success) {
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
                            R.string.catalog_admin_saved,
                            Toast.LENGTH_SHORT).show();
                    success.run();
                });
            } catch (CatalogAdministrationException failure) {
                mainExecutor.execute(() -> {
                    mutationBusy.set(false);
                    showFailure(failure);
                });
            }
        });
    }

    private void showFailure(CatalogAdministrationException failure) {
        if (!isFinishing() && !isDestroyed()) {
            Toast.makeText(
                    this,
                    CatalogAdministrationText.failure(this, failure.kind()),
                    Toast.LENGTH_LONG).show();
        }
    }
}
