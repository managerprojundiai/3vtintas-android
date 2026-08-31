package br.com.tresvtintas.mobile.feature.dashboard;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import br.com.tresvtintas.mobile.core.dashboard.DashboardController;
import br.com.tresvtintas.mobile.core.dashboard.DashboardException;
import br.com.tresvtintas.mobile.core.dashboard.DashboardFailureKind;
import br.com.tresvtintas.mobile.core.dashboard.DashboardState;
import br.com.tresvtintas.mobile.core.dashboard.DashboardStateListener;
import br.com.tresvtintas.mobile.feature.dashboard.databinding.DashboardActivityBinding;
import java.util.Optional;

public final class DashboardActivity extends AppCompatActivity {
    private final DashboardStateListener listener = this::render;
    private DashboardActivityBinding binding;
    private DashboardRenderer renderer;
    private Optional<DashboardController> controller = Optional.empty();

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        DashboardPrivacy.protect(this);
        EdgeToEdge.enable(this);
        binding = DashboardActivityBinding.inflate(getLayoutInflater());
        renderer = new DashboardRenderer(binding);
        setContentView(binding.getRoot());
        DashboardInsets.applySystemBars(binding.getRoot());
        binding.dashboardToolbar.setNavigationOnClickListener(
                ignored -> finish());
        binding.dashboardRefresh.setOnClickListener(
                ignored -> controller.ifPresent(
                        DashboardController::refresh));
        binding.dashboardRetry.setOnClickListener(
                ignored -> controller.ifPresent(
                        DashboardController::load));
    }

    @Override
    protected void onStart() {
        super.onStart();
        Optional<DashboardFeatureRuntime> runtime = runtime();
        if (runtime.isEmpty()) {
            render(DashboardState.error(new DashboardException(
                    DashboardFailureKind.ACCESS_REVOKED,
                    "Dashboard runtime is unavailable.")));
            return;
        }
        DashboardFeatureRuntime value = runtime.orElseThrow();
        DashboardController next = new DashboardController(
                value.repository(),
                value.workerExecutor(),
                ContextCompat.getMainExecutor(this));
        controller = Optional.of(next);
        next.subscribe(listener);
        next.load();
    }

    @Override
    protected void onStop() {
        controller.ifPresent(value -> {
            value.unsubscribe(listener);
            value.close();
        });
        controller = Optional.empty();
        super.onStop();
    }

    private void render(DashboardState state) {
        renderer.render(state);
    }

    private Optional<DashboardFeatureRuntime> runtime() {
        return getApplication() instanceof DashboardRuntimeProvider provider
                ? provider.dashboardRuntime()
                : Optional.empty();
    }
}
