package br.com.tresvtintas.mobile.feature.location;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import br.com.tresvtintas.mobile.core.location.LocationRepository;
import br.com.tresvtintas.mobile.core.location.WorkforceLocationHost;
import br.com.tresvtintas.mobile.core.network.dto.LocationDtos;
import br.com.tresvtintas.mobile.data.location.RemoteLocationRepository;
import br.com.tresvtintas.mobile.feature.location.databinding.ActivityWorkforceLocationBinding;
import br.com.tresvtintas.mobile.platform.location.LocationPermissions;
import br.com.tresvtintas.mobile.platform.location.LocationTrackingController;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class WorkforceLocationActivity extends AppCompatActivity {
    private enum Action {
        ACCEPT,
        FOREGROUND,
        BACKGROUND,
        NOTIFICATION,
        REFRESH
    }

    private final ExecutorService worker = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "3v-location-screen");
        thread.setDaemon(true);
        return thread;
    });
    private final ActivityResultLauncher<String[]> foregroundPermission =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    ignored -> onForegroundPermissionResult());
    private final ActivityResultLauncher<String> backgroundPermission =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> render());
    private final ActivityResultLauncher<String> notificationPermission =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> render());
    private ActivityWorkforceLocationBinding binding;
    private LocationRepository repository;
    private LocationDtos.PolicyResponse snapshot;
    private Action action = Action.REFRESH;
    private boolean destroyed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityWorkforceLocationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.getRoot());
        binding.primaryAction.setOnClickListener(ignored -> performAction());
        binding.revokeAction.setOnClickListener(ignored -> revokeConsent());
        if (!(getApplication() instanceof WorkforceLocationHost currentHost)
                || !currentHost.isWorkforceLocationBuild()
                || currentHost.workforceLocationApi().isEmpty()
                || currentHost.workforceLocationOrganizationId().isEmpty()) {
            unavailable();
            return;
        }
        repository = new RemoteLocationRepository(
                currentHost.workforceLocationOrganizationId().getAsLong(),
                currentHost.workforceLocationApi().orElseThrow());
        load();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (snapshot != null) {
            render();
        }
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        worker.shutdownNow();
        super.onDestroy();
    }

    private void load() {
        loading();
        worker.execute(() -> {
            try {
                LocationDtos.PolicyResponse loaded = repository.policy();
                if (!destroyed) {
                    runOnUiThread(() -> {
                        snapshot = loaded;
                        render();
                        loadHistory();
                    });
                }
            } catch (IOException exception) {
                if (!destroyed) {
                    runOnUiThread(this::error);
                }
            }
        });
    }

    private void render() {
        LocationDtos.PolicyResponse current = snapshot;
        if (current == null) {
            return;
        }
        LocationDtos.Policy policy = current.policy();
        binding.progress.setVisibility(View.GONE);
        binding.disclosure.setText(getString(
                R.string.location_disclosure_body,
                getResources().getQuantityString(
                        R.plurals.location_raw_retention,
                        policy.rawRetentionDays(),
                        policy.rawRetentionDays()),
                getResources().getQuantityString(
                        R.plurals.location_route_retention,
                        policy.routeRetentionDays(),
                        policy.routeRetentionDays())));
        binding.schedule.setText(getString(
                R.string.location_schedule,
                clock(policy.startsAtMinute()),
                clock(policy.endsAtMinute()),
                policy.timeZone()));
        boolean accepted = current.consent() != null
                && "accepted".equals(current.consent().status())
                && current.consent().policyRevision() == policy.revision()
                && policy.disclosureVersion().equals(
                        current.consent().disclosureVersion());
        binding.revokeAction.setVisibility(accepted ? View.VISIBLE : View.GONE);
        if (!accepted) {
            configure(
                    R.string.location_status_consent,
                    R.string.location_consent_action,
                    Action.ACCEPT);
            return;
        }
        if (!LocationPermissions.hasPrecise(this)) {
            configure(
                    R.string.location_status_foreground,
                    R.string.location_foreground_action,
                    Action.FOREGROUND);
            return;
        }
        if (!LocationPermissions.hasBackground(this)) {
            configure(
                    R.string.location_status_background,
                    R.string.location_background_action,
                    Action.BACKGROUND);
            return;
        }
        if (!LocationPermissions.hasNotifications(this)) {
            configure(
                    R.string.location_status_notification,
                    R.string.location_notification_action,
                    Action.NOTIFICATION);
            return;
        }
        LocationTrackingController.enable(this);
        binding.statusTitle.setText(current.scheduleActive()
                ? R.string.location_status_active
                : R.string.location_status_paused);
        binding.statusDetail.setText(WorkforceLocationPresentation.serviceDetail(
                current.scheduleActive(),
                LocationTrackingController.lastServiceState(this)));
        binding.primaryAction.setText(R.string.location_refresh_action);
        binding.primaryAction.setVisibility(View.VISIBLE);
        action = Action.REFRESH;
    }

    private void performAction() {
        if (action == Action.ACCEPT) {
            acceptConsent();
        } else if (action == Action.FOREGROUND) {
            foregroundPermission.launch(new String[] {
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            });
        } else if (action == Action.BACKGROUND) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                backgroundPermission.launch(
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION);
            } else {
                render();
            }
        } else if (action == Action.NOTIFICATION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                render();
            }
        } else {
            load();
        }
    }

    private void acceptConsent() {
        LocationDtos.Policy policy = snapshot.policy();
        loading();
        worker.execute(() -> {
            try {
                repository.consent(
                        policy.revision(),
                        policy.disclosureVersion(),
                        true);
                LocationDtos.PolicyResponse loaded = repository.policy();
                if (!destroyed) {
                    runOnUiThread(() -> {
                        snapshot = loaded;
                        render();
                    });
                }
            } catch (IOException exception) {
                if (!destroyed) {
                    runOnUiThread(this::error);
                }
            }
        });
    }

    private void revokeConsent() {
        LocationDtos.Policy policy = snapshot.policy();
        loading();
        worker.execute(() -> {
            try {
                repository.consent(
                        policy.revision(),
                        policy.disclosureVersion(),
                        false);
                LocationTrackingController.disable(this);
                LocationDtos.PolicyResponse loaded = repository.policy();
                if (!destroyed) {
                    runOnUiThread(() -> {
                        snapshot = loaded;
                        render();
                    });
                }
            } catch (IOException exception) {
                if (!destroyed) {
                    runOnUiThread(this::error);
                }
            }
        });
    }

    private void loadHistory() {
        if (repository == null) {
            return;
        }
        worker.execute(() -> {
            try {
                Instant to = Instant.now();
                LocationDtos.HistoryResponse history = repository.history(
                        to.minusSeconds(24L * 60L * 60L),
                        to,
                        20);
                if (!destroyed) {
                    runOnUiThread(() -> showLastPoint(history));
                }
            } catch (IOException ignored) {
                // Status remains usable when optional history is temporarily unavailable.
            }
        });
    }

    private void showLastPoint(LocationDtos.HistoryResponse history) {
        if (history.items().isEmpty()) {
            binding.lastPoint.setText(R.string.location_last_point_none);
            return;
        }
        LocationDtos.HistoryPoint point = history.items().get(history.items().size() - 1);
        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern("dd/MM HH:mm:ss")
                .withZone(ZoneId.systemDefault());
        binding.lastPoint.setText(getString(
                R.string.location_last_point,
                formatter.format(Instant.parse(point.recordedAt())),
                point.accuracyMeters()));
    }

    private void onForegroundPermissionResult() {
        render();
    }

    private void configure(int status, int button, Action nextAction) {
        binding.statusTitle.setText(status);
        binding.statusDetail.setText("");
        binding.primaryAction.setText(button);
        binding.primaryAction.setVisibility(View.VISIBLE);
        action = nextAction;
    }

    private void loading() {
        binding.statusTitle.setText(R.string.location_status_loading);
        binding.statusDetail.setText("");
        binding.progress.setVisibility(View.VISIBLE);
        binding.primaryAction.setVisibility(View.GONE);
    }

    private void error() {
        binding.progress.setVisibility(View.GONE);
        binding.statusTitle.setText(R.string.location_status_error);
        binding.statusDetail.setText("");
        binding.primaryAction.setVisibility(View.VISIBLE);
        binding.primaryAction.setText(R.string.location_refresh_action);
        action = Action.REFRESH;
    }

    private void unavailable() {
        binding.progress.setVisibility(View.GONE);
        binding.statusTitle.setText(R.string.location_status_unavailable);
        binding.statusDetail.setText("");
        binding.primaryAction.setVisibility(View.GONE);
        binding.revokeAction.setVisibility(View.GONE);
    }

    private static String clock(int minute) {
        return String.format(
                java.util.Locale.ROOT,
                "%02d:%02d",
                minute / 60,
                minute % 60);
    }

    private static void applyInsets(View root) {
        int left = root.getPaddingLeft();
        int top = root.getPaddingTop();
        int right = root.getPaddingRight();
        int bottom = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(
                    left + bars.left,
                    top + bars.top,
                    right + bars.right,
                    bottom + bars.bottom);
            return insets;
        });
    }
}
