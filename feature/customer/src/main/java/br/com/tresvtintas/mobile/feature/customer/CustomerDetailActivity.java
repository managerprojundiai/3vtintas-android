package br.com.tresvtintas.mobile.feature.customer;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import br.com.tresvtintas.mobile.core.customer.CustomerDetailController;
import br.com.tresvtintas.mobile.core.customer.CustomerDetailState;
import br.com.tresvtintas.mobile.core.customer.CustomerDetailStateListener;
import br.com.tresvtintas.mobile.core.customer.CustomerException;
import br.com.tresvtintas.mobile.core.customer.CustomerFailureKind;
import br.com.tresvtintas.mobile.feature.customer.databinding.CustomerActivityDetailBinding;
import java.util.Optional;

public final class CustomerDetailActivity extends AppCompatActivity {
    private static final String EXTRA_CUSTOMER_ID =
            "br.com.tresvtintas.mobile.customer.CUSTOMER_ID";
    private final CustomerDetailStateListener stateListener = this::render;
    private CustomerActivityDetailBinding binding;
    private CustomerDetailRenderer renderer;
    private Optional<CustomerDetailController> controller = Optional.empty();
    private long customerId;

    public static Intent intent(Context context, long customerId) {
        return new Intent(context, CustomerDetailActivity.class)
                .putExtra(EXTRA_CUSTOMER_ID, customerId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CustomerPrivacy.protect(this);
        EdgeToEdge.enable(this);
        binding = CustomerActivityDetailBinding.inflate(getLayoutInflater());
        renderer = new CustomerDetailRenderer(binding);
        setContentView(binding.getRoot());
        CustomerInsets.applySystemBars(binding.getRoot());
        customerId = getIntent().getLongExtra(EXTRA_CUSTOMER_ID, 0);
        binding.customerDetailToolbar.setNavigationOnClickListener(
                ignored -> finish());
        binding.customerDetailRetry.setOnClickListener(ignored -> load());
        binding.customerDetailEdit.setOnClickListener(ignored ->
                startActivity(CustomerEditActivity.intent(this, customerId)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        Optional<CustomerFeatureRuntime> runtime = runtime();
        if (runtime.isEmpty()) {
            render(CustomerDetailState.error(new CustomerException(
                    CustomerFailureKind.ACCESS_REVOKED,
                    "Customer runtime is unavailable.")));
            return;
        }
        CustomerFeatureRuntime available = runtime.orElseThrow();
        renderer.writeAllowed(available.writeAllowed());
        CustomerDetailController next = new CustomerDetailController(
                available.repository(),
                available.workerExecutor(),
                ContextCompat.getMainExecutor(this));
        controller = Optional.of(next);
        next.subscribe(stateListener);
        load();
    }

    @Override
    protected void onStop() {
        controller.ifPresent(value -> {
            value.unsubscribe(stateListener);
            value.close();
        });
        controller = Optional.empty();
        super.onStop();
    }

    private void load() {
        controller.ifPresent(value -> value.load(customerId));
    }

    private void render(CustomerDetailState state) {
        renderer.render(state);
    }

    private Optional<CustomerFeatureRuntime> runtime() {
        if (getApplication() instanceof CustomerRuntimeProvider provider) {
            return provider.customerRuntime();
        }
        return Optional.empty();
    }
}
