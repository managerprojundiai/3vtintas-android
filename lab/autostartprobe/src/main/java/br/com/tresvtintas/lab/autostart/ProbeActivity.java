package br.com.tresvtintas.lab.autostart;

import android.Manifest;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import br.com.tresvtintas.lab.autostart.databinding.ActivityProbeBinding;
import java.util.ArrayList;
import java.util.List;

public final class ProbeActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST = 7303;
    private static final int BACKGROUND_JOB_ID = 7303;
    private static final long JOB_DELAY_MILLIS = 120_000;
    private static final long JOB_DEADLINE_MILLIS = 180_000;
    private ActivityProbeBinding binding;
    private boolean commandHandled;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        binding = ActivityProbeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.probePermissions.setOnClickListener(
                ignored -> requestNextPermission());
        binding.probeImmediate.setOnClickListener(
                ignored -> startVisible());
        binding.probeSchedule.setOnClickListener(
                ignored -> scheduleBackgroundJob());
        binding.probeReset.setOnClickListener(
                ignored -> reset());

        if (state == null) {
            binding.getRoot().post(this::handleCommand);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        commandHandled = false;
        binding.getRoot().post(this::handleCommand);
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void handleCommand() {
        if (commandHandled) {
            return;
        }
        commandHandled = true;
        String command = getIntent().getStringExtra(
                ProbeContract.EXTRA_COMMAND);
        if (ProbeContract.COMMAND_IMMEDIATE.equals(command)) {
            startVisible();
        } else if (ProbeContract.COMMAND_SCHEDULE.equals(command)) {
            scheduleBackgroundJob();
        } else if (ProbeContract.COMMAND_RESET.equals(command)) {
            reset();
        }
    }

    private void startVisible() {
        ProbeStarter.start(this, ProbeContract.SCENARIO_VISIBLE);
        binding.getRoot().postDelayed(this::render, 500);
    }

    private void scheduleBackgroundJob() {
        stopService(new Intent(this, ProbeForegroundService.class));
        ProbeStateStore.reset(this);
        ProbeStateStore.recordScheduled(
                this,
                ProbeContract.SCENARIO_BACKGROUND_JOB);
        JobInfo job = new JobInfo.Builder(
                BACKGROUND_JOB_ID,
                new ComponentName(this, ProbeJobService.class))
                .setMinimumLatency(JOB_DELAY_MILLIS)
                .setOverrideDeadline(JOB_DEADLINE_MILLIS)
                .build();
        int result = getSystemService(JobScheduler.class).schedule(job);
        if (result != JobScheduler.RESULT_SUCCESS) {
            ProbeStateStore.recordScheduleRejected(this);
        }
        render();
        moveTaskToBack(true);
    }

    private void reset() {
        getSystemService(JobScheduler.class).cancel(BACKGROUND_JOB_ID);
        stopService(new Intent(this, ProbeForegroundService.class));
        ProbeStateStore.reset(this);
        render();
    }

    private void requestNextPermission() {
        List<String> foreground = new ArrayList<>();
        if (!granted(Manifest.permission.ACCESS_FINE_LOCATION)) {
            foreground.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            foreground.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && !granted(Manifest.permission.POST_NOTIFICATIONS)) {
            foreground.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (!foreground.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this,
                    foreground.toArray(new String[0]),
                    PERMISSION_REQUEST);
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && !granted(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[] {
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    },
                    PERMISSION_REQUEST);
            return;
        }
        render();
    }

    private boolean granted(String permission) {
        return ActivityCompat.checkSelfPermission(this, permission)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void render() {
        ProbeStateStore.Snapshot snapshot = ProbeStateStore.read(this);
        binding.probeState.setText(getString(
                R.string.probe_state_template,
                snapshot.scenario(),
                snapshot.outcome(),
                snapshot.exceptionClass()));
        binding.probePermissionState.setText(getString(
                R.string.probe_permission_template,
                granted(Manifest.permission.ACCESS_FINE_LOCATION),
                Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                        || granted(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                        || granted(Manifest.permission.POST_NOTIFICATIONS)));
    }
}
