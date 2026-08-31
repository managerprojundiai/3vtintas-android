package br.com.tresvtintas.mobile.feature.catalogadmin;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogImportController;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogImportModels.Batch;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogImportModels.SourceFile;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogImportModels.Status;
import br.com.tresvtintas.mobile.core.catalogadmin.CatalogImportState;
import br.com.tresvtintas.mobile.feature.catalogadmin.databinding.CatalogImportActivityBinding;
import java.io.IOException;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CatalogImportActivity extends AppCompatActivity {
    private static final long POLL_INTERVAL_MILLIS = 1_500L;
    private static final String[] DOCUMENT_TYPES = {
        "text/csv",
        "application/csv",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    };

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean readingFile = new AtomicBoolean();
    private final Runnable poll = this::refreshWhileProcessing;
    private final CatalogImportController.Listener listener = this::render;
    private CatalogImportActivityBinding binding;
    private CatalogImportRowAdapter adapter;
    private CatalogAdministrationFeatureRuntime runtime;
    private CatalogImportController controller;
    private Executor mainExecutor;
    private ActivityResultLauncher<String[]> documentLauncher;
    private CatalogImportState latest = CatalogImportState.empty();

    public static Intent intent(Context context) {
        return new Intent(context, CatalogImportActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CatalogAdministrationPrivacy.protect(this);
        binding = CatalogImportActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mainExecutor = ContextCompat.getMainExecutor(this);
        runtime = runtimeProvider().catalogAdministrationRuntime().orElse(null);
        if (runtime == null) {
            finish();
            return;
        }
        documentLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                this::readSelectedDocument);
        configureViews();
        controller = new CatalogImportController(
                runtime.importRepository(),
                runtime.workerExecutor(),
                mainExecutor);
        controller.subscribe(listener);
    }

    @Override
    protected void onStop() {
        handler.removeCallbacks(poll);
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        latest.batch().ifPresent(batch -> schedulePolling(batch.status()));
    }

    @Override
    protected void onDestroy() {
        if (controller != null) {
            controller.unsubscribe(listener);
            controller.close();
        }
        super.onDestroy();
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
        adapter = new CatalogImportRowAdapter();
        binding.rows.setLayoutManager(new LinearLayoutManager(this));
        binding.rows.setAdapter(adapter);
        binding.back.setOnClickListener(ignored -> finish());
        binding.selectFile.setOnClickListener(
                ignored -> documentLauncher.launch(DOCUMENT_TYPES.clone()));
        binding.upload.setOnClickListener(ignored -> controller.upload());
        binding.refresh.setOnClickListener(ignored -> controller.refresh());
        binding.loadMore.setOnClickListener(ignored -> controller.loadMore());
        binding.confirm.setOnClickListener(ignored -> confirmPreview());
    }

    private void readSelectedDocument(Uri uri) {
        if (uri == null || !readingFile.compareAndSet(false, true)) {
            return;
        }
        binding.progress.setVisibility(View.VISIBLE);
        runtime.workerExecutor().execute(() -> {
            try {
                SourceFile source = CatalogImportFileReader.read(this, uri);
                mainExecutor.execute(() -> {
                    readingFile.set(false);
                    controller.select(source);
                });
            } catch (IOException | IllegalArgumentException failure) {
                mainExecutor.execute(() -> {
                    readingFile.set(false);
                    binding.progress.setVisibility(View.GONE);
                    Toast.makeText(
                            this,
                            fileFailure(failure),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void confirmPreview() {
        Batch batch = latest.batch().orElse(null);
        if (batch == null || !batch.confirmable()) {
            return;
        }
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.catalog_import_confirm_title)
                .setMessage(quantity(
                        R.plurals.catalog_import_confirm_ready,
                        batch.readyRows())
                        + " "
                        + quantity(
                                R.plurals.catalog_import_confirm_invalid,
                                batch.invalidRows()))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(
                        R.string.catalog_import_confirm_action,
                        (ignored, which) -> controller.confirm())
                .create();
        dialog.show();
        CatalogAdministrationPrivacy.protect(dialog);
    }

    private void render(CatalogImportState state) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        latest = state;
        boolean working = state.phase() == CatalogImportState.Phase.WORKING
                || readingFile.get();
        binding.progress.setVisibility(working ? View.VISIBLE : View.GONE);
        binding.selectFile.setEnabled(!working);
        binding.upload.setEnabled(
                !working
                        && state.source().isPresent()
                        && state.batch().isEmpty());
        state.source().ifPresent(source -> {
            binding.fileName.setText(source.name());
            binding.fileSize.setText(getString(
                    R.string.catalog_import_file_size,
                    source.byteCount() / 1024));
        });
        binding.fileCard.setVisibility(
                state.source().isPresent() ? View.VISIBLE : View.GONE);
        adapter.replace(state.rows());
        binding.loadMore.setVisibility(
                state.nextCursor().isPresent() ? View.VISIBLE : View.GONE);
        binding.loadMore.setEnabled(!working);
        renderBatch(state.batch(), working);
        state.failure().ifPresent(failure -> Toast.makeText(
                this,
                CatalogAdministrationText.failure(this, failure.kind()),
                Toast.LENGTH_LONG).show());
    }

    private void renderBatch(Optional<Batch> optional, boolean working) {
        if (optional.isEmpty()) {
            binding.summary.setVisibility(View.GONE);
            binding.refresh.setVisibility(View.GONE);
            binding.confirm.setVisibility(View.GONE);
            return;
        }
        Batch batch = optional.orElseThrow();
        binding.summary.setVisibility(View.VISIBLE);
        binding.refresh.setVisibility(View.VISIBLE);
        binding.status.setText(status(batch.status()));
        binding.counts.setText(getString(
                R.string.catalog_import_count_summary,
                quantity(R.plurals.catalog_import_total_lines, batch.totalRows()),
                quantity(R.plurals.catalog_import_ready_lines, batch.readyRows()),
                quantity(R.plurals.catalog_import_invalid_lines, batch.invalidRows())));
        binding.execution.setText(getString(
                R.string.catalog_import_execution_summary,
                quantity(
                        R.plurals.catalog_import_imported_lines,
                        batch.importedRows()),
                quantity(
                        R.plurals.catalog_import_failed_lines,
                        batch.failedRows())));
        binding.confirm.setVisibility(
                batch.confirmable() ? View.VISIBLE : View.GONE);
        binding.confirm.setEnabled(!working);
        schedulePolling(batch.status());
    }

    private void schedulePolling(Status status) {
        handler.removeCallbacks(poll);
        if (!status.terminal() && status != Status.PREVIEW_READY) {
            handler.postDelayed(poll, POLL_INTERVAL_MILLIS);
        }
    }

    private void refreshWhileProcessing() {
        if (!isFinishing()
                && !isDestroyed()
                && latest.phase() != CatalogImportState.Phase.WORKING) {
            controller.refresh();
        }
    }

    private String status(Status status) {
        int resource = switch (status) {
            case UPLOADED, PREPARING ->
                    R.string.catalog_import_status_preparing;
            case PREVIEW_READY -> R.string.catalog_import_status_preview;
            case QUEUED, RUNNING -> R.string.catalog_import_status_running;
            case COMPLETED -> R.string.catalog_import_status_completed;
            case COMPLETED_WITH_ERRORS ->
                    R.string.catalog_import_status_completed_errors;
            case FAILED -> R.string.catalog_import_status_failed;
            case EXPIRED -> R.string.catalog_import_status_expired;
        };
        return getString(resource);
    }

    private String quantity(int resource, int count) {
        return getResources().getQuantityString(resource, count, count);
    }

    private String fileFailure(Exception failure) {
        String message = Optional.ofNullable(failure.getMessage())
                .orElse("")
                .toUpperCase(Locale.ROOT);
        return getString(message.contains("TOO_LARGE")
                ? R.string.catalog_import_file_too_large
                : R.string.catalog_import_file_invalid);
    }
}
