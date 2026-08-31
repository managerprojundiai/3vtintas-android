package br.com.tresvtintas.mobile.feature.quote;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteDetail;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteException;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteLine;
import br.com.tresvtintas.mobile.core.quote.MaterialQuotePricingSnapshot;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteMutationResult;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteStatus;
import br.com.tresvtintas.mobile.core.quote.MaterialQuoteStatusMutationResult;
import br.com.tresvtintas.mobile.core.order.OrderConversionResult;
import br.com.tresvtintas.mobile.core.order.OrderException;
import br.com.tresvtintas.mobile.feature.quote.databinding.QuoteActivityDetailBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class MaterialQuoteDetailActivity extends AppCompatActivity {
    private static final String EXTRA_QUOTE_ID =
            "br.com.tresvtintas.mobile.quote.QUOTE_ID";
    private static final String EXTRA_AUTO_OPEN_PDF =
            "br.com.tresvtintas.mobile.quote.AUTO_OPEN_PDF";
    private static final String STATE_AUTO_OPEN_PDF =
            "quote_auto_open_pdf";
    private static final String STATE_STATUS_KEY = "quote_status_key";
    private static final String STATE_STATUS_FINGERPRINT =
            "quote_status_fingerprint";
    private static final String STATE_DUPLICATE_KEY = "quote_duplicate_key";
    private static final String STATE_DUPLICATE_SOURCE =
            "quote_duplicate_source";
    private static final String STATE_DUPLICATE_REVISION =
            "quote_duplicate_revision";
    private static final String STATE_ORDER_KEY = "quote_order_key";
    private static final String STATE_ORDER_SOURCE = "quote_order_source";
    private static final String STATE_ORDER_REVISION = "quote_order_revision";
    private QuoteActivityDetailBinding binding;
    private Optional<MaterialQuoteFeatureRuntime> runtime = Optional.empty();
    private Optional<MaterialQuoteDetail> currentDetail = Optional.empty();
    private MaterialQuoteStatusAttempt statusAttempt =
            new MaterialQuoteStatusAttempt();
    private MaterialQuoteDuplicateAttempt duplicateAttempt =
            new MaterialQuoteDuplicateAttempt();
    private MaterialQuoteOrderAttempt orderAttempt = new MaterialQuoteOrderAttempt();
    private long quoteId;
    private long generation;
    private boolean autoOpenPdfPending;

    public static Intent intent(Context context, long quoteId) {
        return new Intent(context, MaterialQuoteDetailActivity.class)
                .putExtra(EXTRA_QUOTE_ID, quoteId);
    }

    public static Intent pdfIntent(Context context, long quoteId) {
        return intent(context, quoteId)
                .putExtra(EXTRA_AUTO_OPEN_PDF, true);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        MaterialQuotePrivacy.protect(this);
        EdgeToEdge.enable(this);
        binding = QuoteActivityDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        quoteId = getIntent().getLongExtra(EXTRA_QUOTE_ID, 0);
        autoOpenPdfPending = state == null
                ? getIntent().getBooleanExtra(
                        EXTRA_AUTO_OPEN_PDF,
                        false)
                : state.getBoolean(STATE_AUTO_OPEN_PDF, false);
        binding.quoteDetailToolbar.setNavigationOnClickListener(
                ignored -> finish());
        binding.quoteEdit.setOnClickListener(ignored -> startActivity(
                MaterialQuoteEditActivity.intent(this, quoteId)));
        binding.quoteStatus.setOnClickListener(ignored -> selectStatus());
        binding.quoteDuplicate.setOnClickListener(ignored -> confirmDuplicate());
        binding.quoteCreateOrder.setOnClickListener(ignored -> confirmCreateOrder());
        binding.quotePdf.setOnClickListener(ignored -> downloadPdf());
        if (state != null) {
            statusAttempt = MaterialQuoteStatusAttempt.restored(
                    state.getString(STATE_STATUS_KEY, ""),
                    state.getString(STATE_STATUS_FINGERPRINT, ""));
            duplicateAttempt = MaterialQuoteDuplicateAttempt.restored(
                    state.getString(STATE_DUPLICATE_KEY, ""),
                    state.getLong(STATE_DUPLICATE_SOURCE),
                    state.getInt(STATE_DUPLICATE_REVISION));
            orderAttempt = MaterialQuoteOrderAttempt.restored(
                    state.getString(STATE_ORDER_KEY, ""),
                    state.getLong(STATE_ORDER_SOURCE),
                    state.getInt(STATE_ORDER_REVISION));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        state.putBoolean(STATE_AUTO_OPEN_PDF, autoOpenPdfPending);
        state.putString(STATE_STATUS_KEY, statusAttempt.key());
        state.putString(
                STATE_STATUS_FINGERPRINT,
                statusAttempt.fingerprint());
        state.putString(STATE_DUPLICATE_KEY, duplicateAttempt.key());
        state.putLong(STATE_DUPLICATE_SOURCE, duplicateAttempt.quoteId());
        state.putInt(STATE_DUPLICATE_REVISION, duplicateAttempt.revision());
        state.putString(STATE_ORDER_KEY, orderAttempt.key());
        state.putLong(STATE_ORDER_SOURCE, orderAttempt.quoteId());
        state.putInt(STATE_ORDER_REVISION, orderAttempt.revision());
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
        currentDetail = Optional.empty();
        super.onStop();
    }

    private void load() {
        if (runtime.isEmpty() || quoteId < 1) {
            binding.quoteDetailError.setText(R.string.quote_failure);
            return;
        }
        generation++;
        long operation = generation;
        binding.quoteDetailProgress.setVisibility(View.VISIBLE);
        MaterialQuoteFeatureRuntime available = runtime.orElseThrow();
        available.workerExecutor().execute(() -> {
            try {
                MaterialQuoteDetail detail =
                        available.quoteRepository().detail(quoteId);
                runOnUiThread(() -> show(operation, detail, available));
            } catch (MaterialQuoteException exception) {
                runOnUiThread(() -> fail(operation));
            }
        });
    }

    private void show(
            long operation,
            MaterialQuoteDetail detail,
            MaterialQuoteFeatureRuntime available) {
        if (operation != generation) {
            return;
        }
        NumberFormat currency = NumberFormat.getCurrencyInstance(
                new Locale("pt", "BR"));
        binding.quoteDetailProgress.setVisibility(View.INVISIBLE);
        currentDetail = Optional.of(detail);
        binding.quoteDetailHeadline.setText(detail.summary().title());
        binding.quoteDetailMeta.setText(getString(
                R.string.quote_detail_meta,
                detail.summary().customer().name(),
                currency.format(detail.summary().total()),
                statusLabel(detail.summary().status()),
                detail.summary().revision()));
        StringBuilder lines = new StringBuilder(256);
        MaterialQuotePricingSnapshot pricing = detail.pricing();
        if (pricing.resolved()) {
            lines.append(getString(
                    R.string.quote_detail_pricing,
                    pricing.priceListName().orElseThrow(),
                    pricing.priceListVersionNumber().orElseThrow(),
                    pricing.priceListCode().orElseThrow()))
                    .append("\n\n");
        } else {
            lines.append(getString(R.string.quote_detail_pricing_legacy))
                    .append("\n\n");
        }
        for (MaterialQuoteLine line : detail.items()) {
            lines.append("• ")
                    .append(line.description())
                    .append(" — ")
                    .append(line.quantity().toPlainString())
                    .append(" × ")
                    .append(currency.format(line.unitPrice()))
                    .append(" = ")
                    .append(currency.format(line.total()))
                    .append('\n');
            line.tint().ifPresent(tint -> lines
                    .append("  ")
                    .append(getString(
                            R.string.quote_detail_tint,
                            tint.colorName(),
                            tint.lineName(),
                            tint.finishName(),
                            tint.packageName(),
                            tint.baseCode()))
                    .append('\n'));
        }
        detail.notes().ifPresent(notes -> lines
                .append("\nObservações\n")
                .append(notes));
        binding.quoteDetailItems.setText(lines.toString().trim());
        binding.quoteEdit.setVisibility(
                available.draftWriteAllowed()
                        && detail.summary().status() == MaterialQuoteStatus.DRAFT
                        ? View.VISIBLE
                        : View.GONE);
        binding.quoteStatus.setVisibility(
                available.statusWriteAllowed()
                        && detail.summary().status()
                                != MaterialQuoteStatus.CONVERTED
                        ? View.VISIBLE
                        : View.GONE);
        binding.quoteDuplicate.setVisibility(
                available.draftWriteAllowed() ? View.VISIBLE : View.GONE);
        binding.quoteCreateOrder.setVisibility(
                available.orderCreateAllowed()
                        && detail.summary().status() == MaterialQuoteStatus.ACCEPTED
                        ? View.VISIBLE : View.GONE);
        int pdfVisibility = available.pdfReadAllowed()
                ? View.VISIBLE
                : View.GONE;
        binding.quotePdf.setVisibility(pdfVisibility);
        binding.quotePdfNote.setVisibility(pdfVisibility);
        binding.quotePdf.setEnabled(true);
        binding.quoteStatus.setEnabled(true);
        binding.quoteDuplicate.setEnabled(true);
        binding.quoteCreateOrder.setEnabled(true);
        binding.quoteDetailError.setText("");
        boolean openPdf = autoOpenPdfPending
                && available.pdfReadAllowed();
        autoOpenPdfPending = false;
        if (openPdf) {
            downloadPdf();
        }
    }

    private void selectStatus() {
        MaterialQuoteDetail detail = currentDetail.orElse(null);
        MaterialQuoteFeatureRuntime available = runtime.orElse(null);
        if (detail == null
                || available == null
                || !available.statusWriteAllowed()
                || detail.summary().status() == MaterialQuoteStatus.CONVERTED) {
            return;
        }
        List<MaterialQuoteStatus> targets = new ArrayList<>();
        for (MaterialQuoteStatus status : MaterialQuoteStatus.values()) {
            if (status != MaterialQuoteStatus.CONVERTED
                    && status != detail.summary().status()) {
                targets.add(status);
            }
        }
        CharSequence[] labels = targets.stream()
                .map(this::statusLabel)
                .toArray(CharSequence[]::new);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.quote_status_title)
                .setItems(labels, (dialog, index) ->
                        confirmStatus(targets.get(index)))
                .setNegativeButton(R.string.quote_cancel, null)
                .show();
    }

    private void confirmStatus(MaterialQuoteStatus target) {
        String message = target == MaterialQuoteStatus.SENT
                ? getString(R.string.quote_status_sent_warning)
                : getString(
                        R.string.quote_status_generic_warning,
                        statusLabel(target));
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.quote_status_confirm_title)
                .setMessage(message)
                .setPositiveButton(
                        R.string.quote_status_confirm,
                        (dialog, ignored) -> transition(target))
                .setNegativeButton(R.string.quote_cancel, null)
                .show();
    }

    private void transition(MaterialQuoteStatus target) {
        MaterialQuoteDetail detail = currentDetail.orElse(null);
        MaterialQuoteFeatureRuntime available = runtime.orElse(null);
        if (detail == null || available == null) {
            return;
        }
        int revision = detail.summary().revision();
        String key = statusAttempt.keyFor(target, revision);
        generation++;
        long operation = generation;
        binding.quoteStatus.setEnabled(false);
        binding.quotePdf.setEnabled(false);
        binding.quoteEdit.setEnabled(false);
        binding.quoteDuplicate.setEnabled(false);
        binding.quoteDetailProgress.setVisibility(View.VISIBLE);
        binding.quoteDetailError.setText("");
        available.workerExecutor().execute(() -> {
            try {
                MaterialQuoteStatusMutationResult result =
                        available.quoteRepository().transition(
                                quoteId,
                                revision,
                                target,
                                key);
                runOnUiThread(() -> statusChanged(operation, result));
            } catch (MaterialQuoteException exception) {
                runOnUiThread(() -> fail(operation));
            }
        });
    }

    private void confirmDuplicate() {
        MaterialQuoteFeatureRuntime available = runtime.orElse(null);
        if (available == null || !available.draftWriteAllowed()) {
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.quote_duplicate_title)
                .setMessage(R.string.quote_duplicate_warning)
                .setPositiveButton(
                        R.string.quote_duplicate_confirm,
                        (dialog, ignored) -> duplicate())
                .setNegativeButton(R.string.quote_cancel, null)
                .show();
    }

    private void duplicate() {
        MaterialQuoteFeatureRuntime available = runtime.orElse(null);
        MaterialQuoteDetail detail = currentDetail.orElse(null);
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
        binding.quoteDetailProgress.setVisibility(View.VISIBLE);
        binding.quoteStatus.setEnabled(false);
        binding.quotePdf.setEnabled(false);
        binding.quoteEdit.setEnabled(false);
        binding.quoteDuplicate.setEnabled(false);
        binding.quoteDetailError.setText("");
        available.workerExecutor().execute(() -> {
            try {
                MaterialQuoteMutationResult result =
                        available.quoteRepository().duplicate(quoteId, key);
                runOnUiThread(() -> duplicated(operation, result));
            } catch (MaterialQuoteException exception) {
                runOnUiThread(() -> fail(operation));
            }
        });
    }

    private void confirmCreateOrder() {
        MaterialQuoteDetail detail = currentDetail.orElse(null);
        MaterialQuoteFeatureRuntime available = runtime.orElse(null);
        if (detail == null || available == null || !available.orderCreateAllowed()
                || detail.summary().status() != MaterialQuoteStatus.ACCEPTED) {
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.quote_create_order_title)
                .setMessage(R.string.quote_create_order_warning)
                .setPositiveButton(R.string.quote_create_order_confirm,
                        (dialog, ignored) -> createOrder())
                .setNegativeButton(R.string.quote_cancel, null)
                .show();
    }

    private void createOrder() {
        MaterialQuoteDetail detail = currentDetail.orElse(null);
        MaterialQuoteFeatureRuntime available = runtime.orElse(null);
        if (detail == null || available == null || !available.orderCreateAllowed()
                || detail.summary().status() != MaterialQuoteStatus.ACCEPTED) {
            return;
        }
        int revision = detail.summary().revision();
        String key = orderAttempt.keyFor(quoteId, revision);
        generation++;
        long operation = generation;
        setActionsEnabled(false);
        binding.quoteDetailProgress.setVisibility(View.VISIBLE);
        binding.quoteDetailError.setText("");
        available.workerExecutor().execute(() -> {
            try {
                OrderConversionResult result = available.orderRepository()
                        .convertMaterialQuote(quoteId, revision, key);
                runOnUiThread(() -> orderCreated(operation, result));
            } catch (OrderException exception) {
                runOnUiThread(() -> orderFailed(operation, exception));
            }
        });
    }

    private void orderCreated(long operation, OrderConversionResult result) {
        if (operation != generation) {
            return;
        }
        orderAttempt.reset();
        binding.quoteStatusNotice.setText(getString(
                R.string.quote_create_order_success,
                Long.toString(result.orderId())));
        binding.quoteStatusNotice.setVisibility(View.VISIBLE);
        load();
    }

    private void duplicated(
            long operation,
            MaterialQuoteMutationResult result) {
        if (operation != generation) {
            return;
        }
        duplicateAttempt.reset();
        startActivity(intent(this, result.quoteId()));
        finish();
    }

    private void statusChanged(
            long operation,
            MaterialQuoteStatusMutationResult result) {
        if (operation != generation) {
            return;
        }
        statusAttempt.reset();
        binding.quoteStatusNotice.setText(getString(
                result.pricingChanged()
                        ? R.string.quote_status_price_updated
                        : R.string.quote_status_updated,
                statusLabel(result.status())));
        binding.quoteStatusNotice.setVisibility(View.VISIBLE);
        load();
    }

    private void downloadPdf() {
        MaterialQuoteFeatureRuntime available = runtime.orElse(null);
        if (available == null || !available.pdfReadAllowed()) {
            return;
        }
        generation++;
        long operation = generation;
        binding.quotePdf.setEnabled(false);
        binding.quoteStatus.setEnabled(false);
        binding.quoteEdit.setEnabled(false);
        binding.quoteDuplicate.setEnabled(false);
        binding.quoteDetailProgress.setVisibility(View.VISIBLE);
        binding.quoteDetailError.setText("");
        available.workerExecutor().execute(() -> {
            try {
                MaterialQuotePdfCache.Artifact artifact =
                        MaterialQuotePdfCache.download(
                                getApplicationContext(),
                                available.quoteRepository(),
                                quoteId);
                runOnUiThread(() -> openPdf(operation, artifact));
            } catch (MaterialQuoteException exception) {
                runOnUiThread(() -> fail(operation));
            }
        });
    }

    private void openPdf(
            long operation,
            MaterialQuotePdfCache.Artifact artifact) {
        if (operation != generation) {
            return;
        }
        binding.quoteDetailProgress.setVisibility(View.INVISIBLE);
        binding.quotePdf.setEnabled(true);
        binding.quoteStatus.setEnabled(true);
        binding.quoteEdit.setEnabled(true);
        binding.quoteDuplicate.setEnabled(true);
        binding.quoteCreateOrder.setEnabled(true);
        startActivity(MaterialQuotePdfViewerActivity.intent(this, artifact));
    }

    private String statusLabel(MaterialQuoteStatus status) {
        return getString(switch (status) {
            case DRAFT -> R.string.quote_status_draft;
            case SENT -> R.string.quote_status_sent;
            case ACCEPTED -> R.string.quote_status_accepted;
            case CONVERTED -> R.string.quote_status_converted;
            case REJECTED -> R.string.quote_status_rejected;
            case EXPIRED -> R.string.quote_status_expired;
        });
    }

    private void fail(long operation) {
        if (operation == generation) {
            binding.quoteDetailProgress.setVisibility(View.INVISIBLE);
            binding.quoteStatus.setEnabled(true);
            binding.quotePdf.setEnabled(true);
            binding.quoteEdit.setEnabled(true);
            binding.quoteDuplicate.setEnabled(true);
            binding.quoteCreateOrder.setEnabled(true);
            binding.quoteDetailError.setText(R.string.quote_failure);
        }
    }

    private void orderFailed(long operation, OrderException exception) {
        if (operation != generation) {
            return;
        }
        MaterialQuoteOrderFailurePresentation presentation =
                MaterialQuoteOrderFailurePresentation.from(exception);
        if (presentation.resetIdempotency()) {
            orderAttempt.reset();
        }
        binding.quoteDetailProgress.setVisibility(View.INVISIBLE);
        setActionsEnabled(true);
        String detail = presentation.serverDetail()
                .orElseGet(() -> getString(presentation.fallbackMessage()));
        binding.quoteDetailError.setText(
                presentation.requestId()
                        .map(requestId -> getString(
                                R.string.quote_failure_with_support,
                                detail,
                                requestId))
                        .orElse(detail));
    }

    private void setActionsEnabled(boolean enabled) {
        binding.quoteStatus.setEnabled(enabled);
        binding.quotePdf.setEnabled(enabled);
        binding.quoteEdit.setEnabled(enabled);
        binding.quoteDuplicate.setEnabled(enabled);
        binding.quoteCreateOrder.setEnabled(enabled);
    }

    private Optional<MaterialQuoteFeatureRuntime> runtime() {
        if (getApplication() instanceof MaterialQuoteRuntimeProvider provider) {
            return provider.materialQuoteRuntime();
        }
        return Optional.empty();
    }
}
