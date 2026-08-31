package br.com.tresvtintas.mobile.platform.location;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import br.com.tresvtintas.mobile.core.location.WorkforceLocationHost;

public final class LocationReconcileWorker extends Worker {
    public LocationReconcileWorker(
            @NonNull Context context,
            @NonNull WorkerParameters parameters) {
        super(context, parameters);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        if (!LocationTrackingController.enabled(context)) {
            return Result.success();
        }
        if (!(context instanceof WorkforceLocationHost host)
                || !host.isWorkforceLocationBuild()
                || host.workforceLocationApi().isEmpty()
                || host.workforceLocationOrganizationId().isEmpty()) {
            return Result.retry();
        }
        if (!LocationPermissions.canStartTrackingService(context)) {
            LocationTrackingController.publishState(context, "permission_required");
            return Result.success();
        }
        try {
            LocationTrackingController.start(context);
            return Result.success();
        } catch (IllegalStateException | SecurityException exception) {
            return Result.retry();
        }
    }
}
