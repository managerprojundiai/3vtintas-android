package br.com.tresvtintas.lab.autostart;

import android.content.Context;
import android.content.Intent;
import androidx.core.content.ContextCompat;

public final class ProbeStarter {
    private ProbeStarter() {
    }

    public static void start(Context context, String scenario) {
        ProbeStateStore.recordAttempt(context, scenario);
        try {
            ContextCompat.startForegroundService(
                    context,
                    new Intent(context, ProbeForegroundService.class));
        } catch (SecurityException | IllegalStateException failure) {
            ProbeStateStore.recordFailure(context, failure);
        }
    }
}
