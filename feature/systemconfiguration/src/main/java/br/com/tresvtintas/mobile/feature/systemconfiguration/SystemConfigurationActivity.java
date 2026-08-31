package br.com.tresvtintas.mobile.feature.systemconfiguration;

import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import br.com.tresvtintas.mobile.core.systemconfiguration.SystemConfigurationController;
import br.com.tresvtintas.mobile.core.systemconfiguration.SystemConfigurationException;
import br.com.tresvtintas.mobile.core.systemconfiguration.SystemConfigurationFailureKind;
import br.com.tresvtintas.mobile.core.systemconfiguration.SystemConfigurationModels.Snapshot;
import br.com.tresvtintas.mobile.core.systemconfiguration.SystemConfigurationModels.Values;
import br.com.tresvtintas.mobile.core.systemconfiguration.SystemConfigurationState;
import br.com.tresvtintas.mobile.core.systemconfiguration.SystemConfigurationStateListener;
import br.com.tresvtintas.mobile.feature.systemconfiguration.databinding.SystemConfigurationActivityBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SystemConfigurationActivity extends AppCompatActivity {
    private final SystemConfigurationStateListener listener = this::render;
    private SystemConfigurationActivityBinding binding;
    private SystemConfigurationRenderer renderer;
    private Optional<SystemConfigurationFeatureRuntime> runtime = Optional.empty();
    private boolean failurePresented;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        EdgeToEdge.enable(this);
        binding = SystemConfigurationActivityBinding.inflate(getLayoutInflater());
        renderer = new SystemConfigurationRenderer(binding);
        setContentView(binding.getRoot());
        applyInsets();
        binding.systemConfigurationToolbar.setNavigationOnClickListener(
                ignored -> finish());
        binding.systemConfigurationRetry.setOnClickListener(
                ignored -> controller().ifPresent(SystemConfigurationController::load));
        binding.systemConfigurationSave.setOnClickListener(ignored -> preview());
    }

    @Override
    protected void onStart() {
        super.onStart();
        runtime = runtimeProvider();
        if (runtime.isEmpty()) {
            Toast.makeText(
                    this,
                    R.string.system_configuration_access_revoked,
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        SystemConfigurationController controller =
                runtime.orElseThrow().controller();
        controller.subscribe(listener);
        if (controller.currentState().phase()
                == SystemConfigurationState.Phase.EMPTY) {
            controller.load();
        }
    }

    @Override
    protected void onStop() {
        controller().ifPresent(value -> value.unsubscribe(listener));
        runtime = Optional.empty();
        super.onStop();
    }

    private void preview() {
        Optional<SystemConfigurationController> current = controller();
        if (current.isEmpty()) {
            return;
        }
        Optional<Snapshot> snapshot = current.orElseThrow()
                .currentState()
                .configuration();
        if (snapshot.isEmpty()) {
            return;
        }
        Values candidate;
        try {
            candidate = renderer.values(normalizedRate(renderer.commissionRate()));
        } catch (IllegalArgumentException exception) {
            Toast.makeText(
                    this,
                    R.string.system_configuration_invalid,
                    Toast.LENGTH_LONG).show();
            return;
        }
        List<String> changes = changes(snapshot.orElseThrow().values(), candidate);
        if (changes.isEmpty()) {
            Toast.makeText(
                    this,
                    R.string.system_configuration_no_changes,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.system_configuration_confirm_title)
                .setMessage(String.join("\n", changes))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(
                        R.string.system_configuration_confirm_action,
                        (dialog, which) -> current.orElseThrow().update(candidate))
                .show();
    }

    private void render(SystemConfigurationState state) {
        renderer.render(state);
        if (state.failure().isPresent() && !failurePresented) {
            failurePresented = true;
            Toast.makeText(
                    this,
                    failureMessage(state.failure().orElseThrow()),
                    Toast.LENGTH_LONG).show();
        } else if (state.failure().isEmpty()) {
            failurePresented = false;
        }
        if (state.phase() == SystemConfigurationState.Phase.READY
                && state.failure().isEmpty()
                && state.changed()) {
            Toast.makeText(
                    this,
                    state.replayed()
                            ? R.string.system_configuration_replayed
                            : R.string.system_configuration_saved,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private int failureMessage(SystemConfigurationException failure) {
        if (failure.kind() == SystemConfigurationFailureKind.CONFLICT) {
            return R.string.system_configuration_conflict;
        }
        if (failure.kind() == SystemConfigurationFailureKind.FORBIDDEN
                || failure.kind() == SystemConfigurationFailureKind.ACCESS_REVOKED) {
            return R.string.system_configuration_access_revoked;
        }
        if (failure.kind() == SystemConfigurationFailureKind.NETWORK) {
            return R.string.system_configuration_network;
        }
        return R.string.system_configuration_failure;
    }

    private List<String> changes(Values before, Values after) {
        List<String> result = new ArrayList<>();
        add(result, R.string.system_configuration_store_name, before.storeName(), after.storeName());
        add(result, R.string.system_configuration_store_phone, before.storePhone(), after.storePhone());
        add(result, R.string.system_configuration_store_address, before.storeAddress(), after.storeAddress());
        add(result, R.string.system_configuration_labor_company, before.laborCompanyName(), after.laborCompanyName());
        add(result, R.string.system_configuration_labor_contact, before.laborCompanyContact(), after.laborCompanyContact());
        add(result, R.string.system_configuration_commission, before.defaultCommissionRate(), after.defaultCommissionRate());
        if (before.autoApprovePainters() != after.autoApprovePainters()) {
            result.add(getString(
                    R.string.system_configuration_change_boolean,
                    getString(R.string.system_configuration_auto_approve),
                    after.autoApprovePainters()
                            ? getString(R.string.system_configuration_enabled)
                            : getString(R.string.system_configuration_disabled)));
        }
        return List.copyOf(result);
    }

    private void add(List<String> changes, int label, String before, String after) {
        if (!before.equals(after)) {
            changes.add(getString(
                    R.string.system_configuration_change,
                    getString(label),
                    display(before),
                    display(after)));
        }
    }

    private String display(String value) {
        return value.isBlank()
                ? getString(R.string.system_configuration_empty)
                : value;
    }

    private static String normalizedRate(String value) {
        try {
            BigDecimal parsed = new BigDecimal(value.trim());
            if (parsed.compareTo(BigDecimal.ZERO) < 0
                    || parsed.compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("Rate is outside range.");
            }
            return parsed.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
        } catch (ArithmeticException | NumberFormatException exception) {
            throw new IllegalArgumentException("Rate is invalid.", exception);
        }
    }

    private Optional<SystemConfigurationController> controller() {
        return runtime.map(SystemConfigurationFeatureRuntime::controller);
    }

    private Optional<SystemConfigurationFeatureRuntime> runtimeProvider() {
        return getApplication() instanceof SystemConfigurationRuntimeProvider provider
                ? provider.systemConfigurationRuntime()
                : Optional.empty();
    }

    private void applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }
}
