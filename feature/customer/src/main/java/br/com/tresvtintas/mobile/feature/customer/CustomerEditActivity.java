package br.com.tresvtintas.mobile.feature.customer;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import br.com.tresvtintas.mobile.core.customer.CustomerDetail;
import br.com.tresvtintas.mobile.core.customer.CustomerDetailController;
import br.com.tresvtintas.mobile.core.customer.CustomerDetailState;
import br.com.tresvtintas.mobile.core.customer.CustomerDetailStateListener;
import br.com.tresvtintas.mobile.core.customer.CustomerDraft;
import br.com.tresvtintas.mobile.core.customer.CustomerFailureKind;
import br.com.tresvtintas.mobile.core.customer.CustomerSaveController;
import br.com.tresvtintas.mobile.core.customer.CustomerSaveState;
import br.com.tresvtintas.mobile.core.customer.CustomerSaveStateListener;
import br.com.tresvtintas.mobile.feature.customer.databinding.CustomerActivityEditBinding;
import java.util.Optional;

public final class CustomerEditActivity extends AppCompatActivity {
    private static final String EXTRA_CUSTOMER_ID =
            "br.com.tresvtintas.mobile.customer.EDIT_CUSTOMER_ID";
    private final CustomerDetailStateListener detailListener =
            this::renderDetail;
    private final CustomerSaveStateListener saveListener = this::renderSave;
    private CustomerActivityEditBinding binding;
    private Optional<CustomerFeatureRuntime> runtime = Optional.empty();
    private Optional<CustomerDetailController> detailController =
            Optional.empty();
    private Optional<CustomerSaveController> saveController = Optional.empty();
    private long customerId;
    private boolean formInitialized;
    private CustomerMutationAttempt mutationAttempt =
            new CustomerMutationAttempt();

    public static Intent intent(Context context, long customerId) {
        return new Intent(context, CustomerEditActivity.class)
                .putExtra(EXTRA_CUSTOMER_ID, customerId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CustomerPrivacy.protect(this);
        EdgeToEdge.enable(this);
        binding = CustomerActivityEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        CustomerInsets.applySystemBars(binding.getRoot());
        customerId = getIntent().getLongExtra(EXTRA_CUSTOMER_ID, 0);
        binding.customerEditToolbar.setTitle(
                customerId > 0
                        ? R.string.customer_edit_title
                        : R.string.customer_create_title);
        binding.customerEditToolbar.setNavigationOnClickListener(
                ignored -> finish());
        binding.customerSave.setOnClickListener(ignored -> save());
        binding.customerEditRetry.setOnClickListener(ignored -> loadDetail());
        restoreRetainedForm();
        if (customerId == 0 && !formInitialized) {
            formInitialized = true;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        runtime = runtime();
        if (runtime.isEmpty() || !runtime.orElseThrow().writeAllowed()) {
            showFailure(CustomerFailureKind.ACCESS_REVOKED, Optional.empty());
            setFormEnabled(false);
            return;
        }
        CustomerFeatureRuntime available = runtime.orElseThrow();
        CustomerSaveController saver = new CustomerSaveController(
                available.repository(),
                available.workerExecutor(),
                ContextCompat.getMainExecutor(this));
        saveController = Optional.of(saver);
        saver.subscribe(saveListener);
        if (customerId > 0 && !formInitialized) {
            CustomerDetailController reader = new CustomerDetailController(
                    available.repository(),
                    available.workerExecutor(),
                    ContextCompat.getMainExecutor(this));
            detailController = Optional.of(reader);
            reader.subscribe(detailListener);
            loadDetail();
        } else {
            setFormEnabled(true);
        }
    }

    @Override
    protected void onStop() {
        detailController.ifPresent(value -> {
            value.unsubscribe(detailListener);
            value.close();
        });
        saveController.ifPresent(value -> {
            value.unsubscribe(saveListener);
            value.close();
        });
        detailController = Optional.empty();
        saveController = Optional.empty();
        runtime = Optional.empty();
        super.onStop();
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        if (!formInitialized) {
            return null;
        }
        return new RetainedForm(
                text(binding.customerName),
                text(binding.customerEmail),
                text(binding.customerPhone),
                text(binding.customerCpf),
                text(binding.customerAddress),
                text(binding.customerCity),
                text(binding.customerState),
                text(binding.customerNotes),
                mutationAttempt.key(),
                mutationAttempt.fingerprint());
    }

    private void loadDetail() {
        binding.customerEditRetry.setVisibility(View.GONE);
        detailController.ifPresent(value -> value.load(customerId));
    }

    private void renderDetail(CustomerDetailState state) {
        binding.customerEditProgress.setVisibility(
                state.phase() == CustomerDetailState.Phase.LOADING
                        ? View.VISIBLE
                        : View.INVISIBLE);
        if (state.phase() == CustomerDetailState.Phase.READY) {
            populate(state.customer().orElseThrow());
            formInitialized = true;
            setFormEnabled(true);
            clearFailure();
        } else if (state.phase() == CustomerDetailState.Phase.ERROR) {
            showFailure(
                    state.failure().orElseThrow(),
                    state.requestId());
            binding.customerEditRetry.setVisibility(View.VISIBLE);
            setFormEnabled(false);
        }
    }

    private void renderSave(CustomerSaveState state) {
        boolean saving = state.phase() == CustomerSaveState.Phase.SAVING;
        binding.customerEditProgress.setVisibility(
                saving ? View.VISIBLE : View.INVISIBLE);
        setFormEnabled(!saving);
        if (state.phase() == CustomerSaveState.Phase.SUCCESS) {
            setResult(RESULT_OK);
            finish();
        } else if (state.phase() == CustomerSaveState.Phase.ERROR) {
            CustomerFailureKind failure = state.failure().orElseThrow();
            showFailure(failure, state.requestId());
            if (!isTransient(failure)) {
                mutationAttempt.reset();
            }
        }
    }

    private void save() {
        if (runtime.isEmpty() || saveController.isEmpty()) {
            showFailure(CustomerFailureKind.ACCESS_REVOKED, Optional.empty());
            return;
        }
        clearFailure();
        CustomerDraft draft;
        try {
            draft = CustomerDraft.fromRaw(
                    text(binding.customerName),
                    text(binding.customerEmail),
                    text(binding.customerPhone),
                    text(binding.customerCpf),
                    text(binding.customerAddress),
                    text(binding.customerCity),
                    text(binding.customerState),
                    text(binding.customerNotes));
        } catch (IllegalArgumentException exception) {
            binding.customerNameLayout.setError(
                    getString(R.string.customer_invalid_form));
            showText(getString(R.string.customer_invalid_form));
            return;
        }
        String idempotencyKey = mutationAttempt.keyFor(draft);
        if (customerId > 0) {
            saveController.orElseThrow().update(
                    customerId,
                    draft,
                    idempotencyKey);
        } else {
            saveController.orElseThrow().create(
                    runtime.orElseThrow().organizationId(),
                    draft,
                    idempotencyKey);
        }
    }

    private void populate(CustomerDetail customer) {
        binding.customerName.setText(customer.name());
        binding.customerEmail.setText(customer.email().orElse(""));
        binding.customerPhone.setText(customer.phone().orElse(""));
        binding.customerCpf.setText(customer.cpf().orElse(""));
        binding.customerAddress.setText(customer.address().orElse(""));
        binding.customerCity.setText(customer.city().orElse(""));
        binding.customerState.setText(customer.state().orElse(""));
        binding.customerNotes.setText(customer.notes().orElse(""));
    }

    private void restoreRetainedForm() {
        Object retained = getLastCustomNonConfigurationInstance();
        if (!(retained instanceof RetainedForm form)) {
            return;
        }
        binding.customerName.setText(form.name());
        binding.customerEmail.setText(form.email());
        binding.customerPhone.setText(form.phone());
        binding.customerCpf.setText(form.cpf());
        binding.customerAddress.setText(form.address());
        binding.customerCity.setText(form.city());
        binding.customerState.setText(form.state());
        binding.customerNotes.setText(form.notes());
        mutationAttempt = CustomerMutationAttempt.restored(
                form.idempotencyKey(),
                form.draftFingerprint());
        formInitialized = true;
    }

    private void setFormEnabled(boolean enabled) {
        binding.customerName.setEnabled(enabled);
        binding.customerEmail.setEnabled(enabled);
        binding.customerPhone.setEnabled(enabled);
        binding.customerCpf.setEnabled(enabled);
        binding.customerAddress.setEnabled(enabled);
        binding.customerCity.setEnabled(enabled);
        binding.customerState.setEnabled(enabled);
        binding.customerNotes.setEnabled(enabled);
        binding.customerSave.setEnabled(enabled);
    }

    private void clearFailure() {
        binding.customerNameLayout.setError(null);
        binding.customerEditError.setText("");
        binding.customerEditError.setVisibility(View.GONE);
    }

    private void showFailure(
            CustomerFailureKind failure,
            Optional<String> requestId) {
        String message = getString(CustomerFailureText.resource(failure));
        if (requestId.isPresent()) {
            message += "\n\n" + getString(
                    R.string.customer_support_code,
                    requestId.orElseThrow());
        }
        showText(message);
    }

    private void showText(String message) {
        binding.customerEditError.setText(message);
        binding.customerEditError.setVisibility(View.VISIBLE);
    }

    private Optional<CustomerFeatureRuntime> runtime() {
        if (getApplication() instanceof CustomerRuntimeProvider provider) {
            return provider.customerRuntime();
        }
        return Optional.empty();
    }

    private static boolean isTransient(CustomerFailureKind failure) {
        return switch (failure) {
            case IDEMPOTENCY_IN_PROGRESS, NETWORK, RATE_LIMITED,
                    SERVICE_UNAVAILABLE -> true;
            default -> false;
        };
    }

    private static String text(android.widget.EditText field) {
        return field.getText() == null ? "" : field.getText().toString();
    }

    private record RetainedForm(
            String name,
            String email,
            String phone,
            String cpf,
            String address,
            String city,
            String state,
            String notes,
            String idempotencyKey,
            String draftFingerprint) {
    }
}
