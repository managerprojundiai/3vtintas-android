package br.com.tresvtintas.mobile.feature.notifications;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import br.com.tresvtintas.mobile.core.notifications.NotificationException;
import br.com.tresvtintas.mobile.core.notifications.NotificationFailureKind;
import br.com.tresvtintas.mobile.core.notifications.NotificationPermissionController;
import br.com.tresvtintas.mobile.core.notifications.NotificationPermissionState;
import br.com.tresvtintas.mobile.core.notifications.NotificationSettingsController;
import br.com.tresvtintas.mobile.core.notifications.NotificationSettingsState;
import br.com.tresvtintas.mobile.core.notifications.NotificationSettingsStateListener;
import br.com.tresvtintas.mobile.feature.notifications.databinding.NotificationSettingsActivityBinding;
import java.util.Optional;

public final class NotificationSettingsActivity extends AppCompatActivity {
    private final NotificationSettingsStateListener listener = this::render;
    private NotificationSettingsActivityBinding binding;
    private NotificationSettingsRenderer renderer;
    private ActivityResultLauncher<String> permissionLauncher;
    private Optional<NotificationFeatureRuntime> runtime = Optional.empty();

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        EdgeToEdge.enable(this);
        binding = NotificationSettingsActivityBinding.inflate(getLayoutInflater());
        renderer = new NotificationSettingsRenderer(binding);
        setContentView(binding.getRoot());
        NotificationSettingsInsets.applySystemBars(binding.getRoot());
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                this::permissionResult);
        binding.notificationToolbar.setNavigationOnClickListener(
                ignored -> finish());
        binding.notificationRetry.setOnClickListener(
                ignored -> controller().ifPresent(
                        NotificationSettingsController::load));
        binding.notificationSave.setOnClickListener(
                ignored -> save());
        binding.notificationPermissionAction.setOnClickListener(
                ignored -> requestPermission());
    }

    @Override
    protected void onStart() {
        super.onStart();
        runtime = runtimeProvider();
        if (runtime.isEmpty()) {
            render(NotificationSettingsState.error(new NotificationException(
                    NotificationFailureKind.ACCESS_REVOKED,
                    "Notification runtime is unavailable.")));
            return;
        }
        NotificationSettingsController controller =
                runtime.orElseThrow().controller();
        controller.subscribe(listener);
        if (controller.currentState().phase()
                == NotificationSettingsState.Phase.EMPTY) {
            controller.load();
        } else {
            reconcilePermission();
        }
    }

    @Override
    protected void onStop() {
        controller().ifPresent(value -> value.unsubscribe(listener));
        runtime = Optional.empty();
        super.onStop();
    }

    private void save() {
        Optional<NotificationSettingsController> current = controller();
        if (current.isEmpty()) {
            return;
        }
        NotificationPermissionState permission =
                permissionController().map(
                                NotificationPermissionController::currentState)
                        .orElse(NotificationPermissionState.UNKNOWN);
        current.orElseThrow().update(
                permission,
                renderer.operationalEnabled(),
                renderer.selection());
    }

    private void requestPermission() {
        Optional<NotificationPermissionController> gateway =
                permissionController();
        if (gateway.isEmpty()) {
            return;
        }
        NotificationPermissionController current = gateway.orElseThrow();
        if (!current.requiresRuntimeRequest()) {
            if (current.currentState() == NotificationPermissionState.GRANTED) {
                save();
            } else {
                openSystemSettings();
            }
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && current.currentState() == NotificationPermissionState.DENIED
                && !shouldShowRequestPermissionRationale(
                        Manifest.permission.POST_NOTIFICATIONS)) {
            openSystemSettings();
            return;
        }
        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    private void permissionResult(boolean granted) {
        permissionController().ifPresent(gateway -> {
            gateway.recordDecision(granted);
            save();
        });
    }

    private void reconcilePermission() {
        Optional<NotificationSettingsController> current = controller();
        Optional<NotificationPermissionController> gateway =
                permissionController();
        if (current.isEmpty()
                || gateway.isEmpty()
                || current.orElseThrow().currentState().preferences().isEmpty()) {
            return;
        }
        NotificationPermissionState platform =
                gateway.orElseThrow().currentState();
        NotificationPermissionState stored = current.orElseThrow()
                .currentState()
                .preferences()
                .orElseThrow()
                .permissionState();
        if (platform != NotificationPermissionState.UNKNOWN
                && platform != stored) {
            save();
        }
    }

    private void openSystemSettings() {
        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName())
                .setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    private void render(NotificationSettingsState state) {
        renderer.render(
                state,
                runtime.map(NotificationFeatureRuntime::pushAvailable)
                        .orElse(false));
    }

    private Optional<NotificationSettingsController> controller() {
        return runtime.map(NotificationFeatureRuntime::controller);
    }

    private Optional<NotificationPermissionController> permissionController() {
        return runtime.map(NotificationFeatureRuntime::permissionController);
    }

    private Optional<NotificationFeatureRuntime> runtimeProvider() {
        return getApplication() instanceof NotificationRuntimeProvider provider
                ? provider.notificationRuntime()
                : Optional.empty();
    }
}
