package br.com.tresvtintas.mobile.feature.laborquote;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteDetail;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteException;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteLine;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteMutationResult;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteStatus;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteStatusMutationResult;
import br.com.tresvtintas.mobile.feature.laborquote.databinding.LaborQuoteActivityDetailBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class LaborQuoteDetailActivity extends AppCompatActivity {
    private static final String EXTRA_QUOTE_ID =
            "br.com.tresvtintas.mobile.laborquote.QUOTE_ID";
    private static final String EXTRA_AUTO_OPEN_PDF =
            "br.com.tresvtintas.mobile.laborquote.AUTO_OPEN_PDF";
    private static final String STATE_AUTO_OPEN_PDF =
            "labor_quote_auto_open_pdf";
    private static final String STATE_STATUS_KEY = "labor_quote_status_key";
    private static final String STATE_STATUS_FINGERPRINT =
            "labor_quote_status_fingerprint";
    private static final String STATE_DUPLICATE_KEY =
            "labor_quote_duplicate_key";
    private static final String STATE_DUPLICATE_SOURCE =
            "labor_quote_duplicate_source";
    private static final String STATE_DUPLICATE_REVISION =
            "labor_quote_duplicate_revision";
    private LaborQuoteActivityDetailBinding binding;
    private Optional<LaborQuoteFeatureRuntime> runtime = Optional.empty();
    private Optional<LaborQuoteDetail> current = Optional.empty();
    private LaborQuoteStatusAttempt statusAttempt = new LaborQuoteStatusAttempt();
    private LaborQuoteDuplicateAttempt duplicateAttempt =
            new LaborQuoteDuplicateAttempt();
    private long quoteId;
    private long generation;
    private boolean autoOpenPdfPending;

    public static Intent intent(Context context, long quoteId) {
        return new Intent(context, LaborQuoteDetailActivity.class)
                .putExtra(EXTRA_QUOTE_ID, quoteId);
    }

    public static Intent pdfIntent(Context context, long quoteId) {
        return intent(context, quoteId)
                .putExtra(EXTRA_AUTO_OPEN_PDF, true);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        LaborQuotePrivacy.protect(this);
        EdgeToEdge.enable(this);
        binding = LaborQuoteActivityDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        quoteId = getIntent().getLongExtra(EXTRA_QUOTE_ID, 0);
        autoOpenPdfPending = state == null
                ? getIntent().getBooleanExtra(
                        EXTRA_AUTO_OPEN_PDF,
                        false)
                : state.getBoolean(STATE_AUTO_OPEN_PDF, false);
        binding.laborQuoteDetailToolbar.setNavigationOnClickListener(ignored -> finish());
        binding.laborQuoteEdit.setOnClickListener(ignored -> startActivity(
                LaborQuoteEditActivity.intent(this, quoteId)));
        binding.laborQuoteStatus.setOnClickListener(ignored -> selectStatus());
        binding.laborQuoteDuplicate.setOnClickListener(
                ignored -> confirmDuplicate());
        binding.laborQuotePdf.setOnClickListener(ignored -> downloadPdf());
        if (state != null) {
            statusAttempt = LaborQuoteStatusAttempt.restored(
                    state.getString(STATE_STATUS_KEY, ""),
                    state.getString(STATE_STATUS_FINGERPRINT, ""));
            duplicateAttempt = LaborQuoteDuplicateAttempt.restored(
                    state.getString(STATE_DUPLICATE_KEY, ""),
                    state.getLong(STATE_DUPLICATE_SOURCE),
                    state.getInt(STATE_DUPLICATE_REVISION));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        state.putBoolean(STATE_AUTO_OPEN_PDF, autoOpenPdfPending);
        state.putString(STATE_STATUS_KEY, statusAttempt.key());
        state.putString(STATE_STATUS_FINGERPRINT, statusAttempt.fingerprint());
        state.putString(STATE_DUPLICATE_KEY, duplicateAttempt.key());
        state.putLong(STATE_DUPLICATE_SOURCE, duplicateAttempt.quoteId());
        state.putInt(STATE_DUPLICATE_REVISION, duplicateAttempt.revision());
        super.onSaveInstanceState(state);
    }

    @Override
    protected void onStart() {
        super.onStart();
        runtime = runtime();
        load();
    }

    @Override
    protected void onStop() {
        generation++;
        runtime = Optional.empty();
        current = Optional.empty();
        super.onStop();
    }

    private void load() {
        LaborQuoteFeatureRuntime available = runtime.orElse(null);
        if (available == null || quoteId < 1) {
            binding.laborQuoteDetailError.setText(R.string.labor_quote_failure);
            return;
        }
        generation++;
        long operation = generation;
        busy(true);
        available.workerExecutor().execute(() -> {
            try {
                LaborQuoteDetail detail = available.quoteRepository().detail(quoteId);
                runOnUiThread(() -> show(operation, detail, available));
            } catch (LaborQuoteException exception) {
                runOnUiThread(() -> fail(operation));
            }
        });
    }

    private void show(
            long operation,
            LaborQuoteDetail detail,
            LaborQuoteFeatureRuntime available) {
        if (operation != generation) {
            return;
        }
        busy(false);
        current = Optional.of(detail);
        NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        binding.laborQuoteDetailHeadline.setText(detail.summary().title());
        binding.laborQuoteDetailMeta.setText(getString(
                R.string.labor_quote_detail_meta,
                detail.summary().customer().name(),
                currency.format(detail.summary().total()),
                detail.summary().painter().name(),
                getString(LaborQuoteUi.statusLabel(detail.summary().status())),
                detail.summary().revision()));
        binding.laborQuoteDetailItems.setText(lines(detail, currency));
        binding.laborQuoteEdit.setVisibility(
                available.draftWriteAllowed()
                                && detail.summary().status() == LaborQuoteStatus.DRAFT
                        ? View.VISIBLE
                        : View.GONE);
        binding.laborQuoteStatus.setVisibility(
                available.statusWriteAllowed()
                                && detail.summary().status() != LaborQuoteStatus.CONVERTED
                        ? View.VISIBLE
                        : View.GONE);
        binding.laborQuoteDuplicate.setVisibility(
                available.draftWriteAllowed() ? View.VISIBLE : View.GONE);
        int pdfVisibility = available.pdfReadAllowed() ? View.VISIBLE : View.GONE;
        binding.laborQuotePdf.setVisibility(pdfVisibility);
        binding.laborQuotePdfNote.setVisibility(pdfVisibility);
        setActionsEnabled(true);
        binding.laborQuoteDetailError.setText("");
        boolean openPdf = autoOpenPdfPending
                && available.pdfReadAllowed();
        autoOpenPdfPending = false;
        if (openPdf) {
            downloadPdf();
        }
    }

    private static String lines(LaborQuoteDetail detail, NumberFormat currency) {
        StringBuilder text = new StringBuilder(320);
        for (LaborQuoteLine line : detail.items()) {
            text.append("• ")
                    .append(line.description())
                    .append(" — ")
                    .append(line.quantity().toPlainString())
                    .append(' ')
                    .append(line.unit())
                    .append(" × ")
                    .append(currency.format(line.unitPrice()))
                    .append(" = ")
                    .append(currency.format(line.total()))
                    .append('\n');
        }
        detail.notes().ifPresent(notes -> text.append("\nObservações\n").append(notes));
        return text.toString().trim();
    }

    private void selectStatus() {
        LaborQuoteDetail detail = current.orElse(null);
        LaborQuoteFeatureRuntime available = runtime.orElse(null);
        if (detail == null || available == null || !available.statusWriteAllowed()) {
            return;
        }
        List<LaborQuoteStatus> targets = new ArrayList<>();
        for (LaborQuoteStatus status : LaborQuoteStatus.values()) {
            if (status != LaborQuoteStatus.CONVERTED && status != detail.summary().status()) {
                targets.add(status);
            }
        }
        CharSequence[] labels = targets.stream()
                .map(status -> getString(LaborQuoteUi.statusLabel(status)))
                .toArray(CharSequence[]::new);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.labor_quote_status_title)
                .setItems(labels, (dialog, index) -> confirmStatus(targets.get(index)))
                .setNegativeButton(R.string.labor_quote_cancel, null)
                .show();
    }

    private void confirmStatus(LaborQuoteStatus status) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.labor_quote_status_confirm_title)
                .setMessage(getString(
                        R.string.labor_quote_status_warning,
                        getString(LaborQuoteUi.statusLabel(status))))
                .setPositiveButton(
                        R.string.labor_quote_status_confirm,
                        (dialog, ignored) -> transition(status))
                .setNegativeButton(R.string.labor_quote_cancel, null)
                .show();
    }

    private void transition(LaborQuoteStatus status) {
        LaborQuoteDetail detail = current.orElse(null);
        LaborQuoteFeatureRuntime available = runtime.orElse(null);
        if (detail == null || available == null) {
            return;
        }
        int revision = detail.summary().revision();
        String key = statusAttempt.keyFor(status, revision);
        generation++;
        long operation = generation;
        busy(true);
        setActionsEnabled(false);
        available.workerExecutor().execute(() -> {
            try {
                LaborQuoteStatusMutationResult result = available.quoteRepository().transition(
                        quoteId,
                        revision,
                        status,
                        key);
                runOnUiThread(() -> statusChanged(operation, result));
            } catch (LaborQuoteException exception) {
                runOnUiThread(() -> fail(operation));
            }
        });
    }

    private void statusChanged(long operation, LaborQuoteStatusMutationResult result) {
        if (operation == generation) {
            statusAttempt.reset();
            binding.laborQuoteStatusNotice.setText(getString(
                    R.string.labor_quote_status_updated,
                    getString(LaborQuoteUi.statusLabel(result.status()))));
            binding.laborQuoteStatusNotice.setVisibility(View.VISIBLE);
            load();
        }
    }

    private void confirmDuplicate() {
        LaborQuoteFeatureRuntime available = runtime.orElse(null);
        if (available == null || !available.draftWriteAllowed()) {
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.labor_quote_duplicate_title)
                .setMessage(R.string.labor_quote_duplicate_warning)
                .setPositiveButton(
                        R.string.labor_quote_duplicate_confirm,
                        (dialog, ignored) -> duplicate())
                .setNegativeButton(R.string.labor_quote_cancel, null)
                .show();
    }

    private void duplicate() {
        LaborQuoteFeatureRuntime available = runtime.orElse(null);
        LaborQuoteDetail detail = current.orElse(null);
        if (available == null
                || detail == null
                || !available.draftWriteAllowed()) {
            return;
        }
        String key = duplicateAttempt.keyFor(
                quoteId,
                detail.summary().revision());
        generation++;
        long operation = generation;
        busy(true);
        setActionsEnabled(false);
        binding.laborQuoteDetailError.setText("");
        available.workerExecutor().execute(() -> {
            try {
                LaborQuoteMutationResult result =
                        available.quoteRepository().duplicate(quoteId, key);
                runOnUiThread(() -> duplicated(operation, result));
            } catch (LaborQuoteException exception) {
                runOnUiThread(() -> fail(operation));
            }
        });
    }

    private void duplicated(long operation, LaborQuoteMutationResult result) {
        if (operation != generation) {
            return;
        }
        duplicateAttempt.reset();
        startActivity(intent(this, result.quoteId()));
        finish();
    }

    private void downloadPdf() {
        LaborQuoteFeatureRuntime available = runtime.orElse(null);
        if (available == null || !available.pdfReadAllowed()) {
            return;
        }
        generation++;
        long operation = generation;
        busy(true);
        setActionsEnabled(false);
        available.workerExecutor().execute(() -> {
            try {
                LaborQuotePdfCache.Artifact artifact = LaborQuotePdfCache.download(
                        getApplicationContext(),
                        available.quoteRepository(),
                        quoteId);
                runOnUiThread(() -> openPdf(operation, artifact));
            } catch (LaborQuoteException exception) {
                runOnUiThread(() -> fail(operation));
            }
        });
    }

    private void openPdf(long operation, LaborQuotePdfCache.Artifact artifact) {
        if (operation == generation) {
            busy(false);
            setActionsEnabled(true);
            startActivity(LaborQuotePdfViewerActivity.intent(this, artifact));
        }
    }

    private void fail(long operation) {
        if (operation == generation) {
            busy(false);
            setActionsEnabled(true);
            binding.laborQuoteDetailError.setText(R.string.labor_quote_failure);
        }
    }

    private void busy(boolean value) {
        binding.laborQuoteDetailProgress.setVisibility(value ? View.VISIBLE : View.INVISIBLE);
    }

    private void setActionsEnabled(boolean enabled) {
        binding.laborQuoteEdit.setEnabled(enabled);
        binding.laborQuoteStatus.setEnabled(enabled);
        binding.laborQuotePdf.setEnabled(enabled);
        binding.laborQuoteDuplicate.setEnabled(enabled);
    }

    private Optional<LaborQuoteFeatureRuntime> runtime() {
        if (getApplication() instanceof LaborQuoteRuntimeProvider provider) {
            return provider.laborQuoteRuntime();
        }
        return Optional.empty();
    }
}
