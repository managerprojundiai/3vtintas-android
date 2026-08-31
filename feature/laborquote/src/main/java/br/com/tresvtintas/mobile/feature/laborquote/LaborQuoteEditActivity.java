package br.com.tresvtintas.mobile.feature.laborquote;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import br.com.tresvtintas.mobile.core.customer.CustomerSummary;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteDetail;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteDraft;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteDraftLine;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteException;
import br.com.tresvtintas.mobile.core.laborquote.LaborQuoteFailureKind;
import br.com.tresvtintas.mobile.feature.laborquote.databinding.LaborQuoteActivityEditBinding;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.stream.Collectors;

public final class LaborQuoteEditActivity extends AppCompatActivity {
    private static final String EXTRA_QUOTE_ID =
            "br.com.tresvtintas.mobile.laborquote.EDIT_QUOTE_ID";
    private LaborQuoteActivityEditBinding binding;
    private LaborQuoteEditorView editorView;
    private LaborQuoteCustomerPicker customerPicker;
    private Optional<LaborQuoteFeatureRuntime> runtime = Optional.empty();
    private Optional<CustomerSummary> selectedCustomer = Optional.empty();
    private LaborQuoteEditorModel editor = new LaborQuoteEditorModel();
    private LaborQuoteMutationAttempt mutationAttempt = new LaborQuoteMutationAttempt();
    private long quoteId;
    private int revision;
    private long generation;
    private boolean initialized;

    public static Intent intent(Context context, long quoteId) {
        return new Intent(context, LaborQuoteEditActivity.class)
                .putExtra(EXTRA_QUOTE_ID, quoteId);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        LaborQuotePrivacy.protect(this);
        EdgeToEdge.enable(this);
        binding = LaborQuoteActivityEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        editorView = new LaborQuoteEditorView(this, binding);
        customerPicker = new LaborQuoteCustomerPicker(this, editorView, customer -> {
            selectedCustomer = Optional.of(customer);
            mutationAttempt.reset();
            render();
        });
        quoteId = getIntent().getLongExtra(EXTRA_QUOTE_ID, 0);
        binding.laborQuoteEditToolbar.setTitle(
                quoteId > 0
                        ? R.string.labor_quote_edit_title
                        : R.string.labor_quote_create_title);
        binding.laborQuoteEditToolbar.setNavigationOnClickListener(ignored -> finish());
        binding.laborQuoteCustomer.setOnClickListener(ignored -> chooseCustomer());
        binding.laborQuoteAddService.setOnClickListener(ignored -> editLine(-1));
        binding.laborQuoteItems.setOnItemClickListener(
                (parent, view, position, id) -> editLine(position));
        binding.laborQuoteSave.setOnClickListener(ignored -> save());
        editorView.observeDiscount(() -> {
            if (initialized) {
                mutationAttempt.reset();
                render();
            }
        });
        restoreRetained();
    }

    @Override
    protected void onStart() {
        super.onStart();
        runtime = runtime();
        if (runtime.isEmpty() || !runtime.orElseThrow().draftWriteAllowed()) {
            showFailure();
            editorView.setEnabled(false, quoteId == 0);
            return;
        }
        if (!initialized) {
            load();
        } else {
            editorView.setEnabled(true, quoteId == 0);
        }
    }

    @Override
    protected void onStop() {
        generation++;
        customerPicker.stop();
        runtime = Optional.empty();
        super.onStop();
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        if (!initialized) {
            return null;
        }
        return new LaborQuoteEditorSnapshot(
                selectedCustomer.orElse(null),
                editor.lines(),
                editorView.title(),
                editorView.notes(),
                editorView.discountText(),
                revision,
                mutationAttempt.key(),
                mutationAttempt.fingerprint());
    }

    private void load() {
        LaborQuoteFeatureRuntime available = runtime.orElseThrow();
        generation++;
        long operation = generation;
        editorView.setEnabled(false, quoteId == 0);
        editorView.progress(true);
        editorView.message(R.string.labor_quote_loading);
        available.workerExecutor().execute(() -> {
            try {
                LaborQuoteDetail detail = quoteId > 0
                        ? available.quoteRepository().detail(quoteId)
                        : null;
                runOnUiThread(() -> initialize(operation, detail));
            } catch (LaborQuoteException exception) {
                runOnUiThread(() -> failLoad(operation));
            }
        });
    }

    private void initialize(long operation, LaborQuoteDetail detail) {
        if (operation != generation) {
            return;
        }
        if (detail != null) {
            revision = detail.summary().revision();
            selectedCustomer = Optional.of(summaryFromDetail(detail));
            editorView.restoreText(
                    detail.summary().title(),
                    detail.notes().orElse(""),
                    detail.summary().discount().toPlainString());
            editor = new LaborQuoteEditorModel(detail.items().stream()
                    .map(item -> new LaborQuoteDraftLine(
                            item.description(),
                            item.quantity(),
                            item.unit(),
                            item.unitPrice()))
                    .collect(Collectors.toList()));
        }
        initialized = true;
        editorView.progress(false);
        editorView.clearMessage();
        render();
        editorView.setEnabled(true, quoteId == 0);
    }

    private static CustomerSummary summaryFromDetail(LaborQuoteDetail detail) {
        return new CustomerSummary(
                detail.summary().customer().id(),
                detail.summary().organizationId(),
                detail.summary().customer().name(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                OptionalLong.empty(),
                detail.summary().createdAt(),
                detail.summary().updatedAt());
    }

    private void chooseCustomer() {
        if (quoteId == 0) {
            customerPicker.choose(runtime);
        }
    }

    private void editLine(int index) {
        LaborQuoteDraftLine existing = index >= 0 ? editor.lines().get(index) : null;
        LaborQuoteLineDialog.show(this, existing, new LaborQuoteLineDialog.Listener() {
            @Override
            public void save(LaborQuoteDraftLine line) {
                if (index >= 0) {
                    editor.replace(index, line);
                } else {
                    editor.add(line);
                }
                changed();
            }

            @Override
            public void remove() {
                if (index >= 0) {
                    editor.remove(index);
                    changed();
                }
            }
        });
    }

    private void changed() {
        mutationAttempt.reset();
        editorView.clearMessage();
        render();
    }

    private void render() {
        editorView.render(selectedCustomer, editor);
    }

    private void save() {
        LaborQuoteFeatureRuntime available = runtime.orElse(null);
        CustomerSummary customer = selectedCustomer.orElse(null);
        if (available == null || customer == null || editor.lines().isEmpty()) {
            editorView.message(R.string.labor_quote_invalid);
            return;
        }
        LaborQuoteDraft draft;
        try {
            draft = new LaborQuoteDraft(
                    customer.id(),
                    customer.name(),
                    Optional.of(editorView.title()),
                    Optional.of(editorView.notes()),
                    Optional.empty(),
                    editorView.discount(),
                    editor.lines());
        } catch (IllegalArgumentException exception) {
            editorView.message(R.string.labor_quote_invalid);
            return;
        }
        String key = mutationAttempt.keyFor(draft, revision);
        generation++;
        long operation = generation;
        editorView.setEnabled(false, quoteId == 0);
        editorView.progress(true);
        available.workerExecutor().execute(() -> saveRemote(operation, available, draft, key));
    }

    private void saveRemote(
            long operation,
            LaborQuoteFeatureRuntime available,
            LaborQuoteDraft draft,
            String key) {
        try {
            if (quoteId > 0) {
                available.quoteRepository().update(quoteId, revision, draft, key);
            } else {
                available.quoteRepository().create(draft, key);
            }
            runOnUiThread(() -> finishSuccess(operation));
        } catch (LaborQuoteException exception) {
            runOnUiThread(() -> finishFailure(operation, exception));
        }
    }

    private void finishSuccess(long operation) {
        if (operation == generation) {
            setResult(RESULT_OK);
            finish();
        }
    }

    private void finishFailure(long operation, LaborQuoteException exception) {
        if (operation != generation) {
            return;
        }
        editorView.progress(false);
        editorView.setEnabled(true, quoteId == 0);
        if (!transientFailure(exception.kind())) {
            mutationAttempt.reset();
        }
        showFailure();
    }

    private void failLoad(long operation) {
        if (operation == generation) {
            editorView.progress(false);
            showFailure();
        }
    }

    private void showFailure() {
        editorView.message(R.string.labor_quote_failure);
    }

    private void restoreRetained() {
        Object retained = getLastCustomNonConfigurationInstance();
        if (!(retained instanceof LaborQuoteEditorSnapshot value)) {
            return;
        }
        selectedCustomer = Optional.ofNullable(value.customer());
        editor = new LaborQuoteEditorModel(value.lines());
        editorView.restoreText(value.title(), value.notes(), value.discount());
        revision = value.revision();
        mutationAttempt = LaborQuoteMutationAttempt.restored(
                value.idempotencyKey(),
                value.fingerprint());
        initialized = true;
        render();
    }

    private Optional<LaborQuoteFeatureRuntime> runtime() {
        if (getApplication() instanceof LaborQuoteRuntimeProvider provider) {
            return provider.laborQuoteRuntime();
        }
        return Optional.empty();
    }

    private static boolean transientFailure(LaborQuoteFailureKind kind) {
        return switch (kind) {
            case IDEMPOTENCY_IN_PROGRESS, NETWORK, RATE_LIMITED, SERVICE_UNAVAILABLE -> true;
            default -> false;
        };
    }

}
