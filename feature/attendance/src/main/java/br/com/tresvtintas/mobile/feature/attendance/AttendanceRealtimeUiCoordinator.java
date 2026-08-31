package br.com.tresvtintas.mobile.feature.attendance;

import android.view.View;
import android.widget.TextView;
import br.com.tresvtintas.mobile.core.attendance.AttendanceRealtimeCoordinator;
import br.com.tresvtintas.mobile.core.attendance.AttendanceRealtimeState;
import br.com.tresvtintas.mobile.core.attendance.AttendanceRealtimeSubscription;
import java.util.Objects;
import java.util.Optional;

/**
 * Binds the session-scoped realtime coordinator to one visible Attendance screen.
 */
final class AttendanceRealtimeUiCoordinator {
    private final TextView status;
    private Optional<AttendanceRealtimeSubscription> observation =
            Optional.empty();
    private long generation;

    AttendanceRealtimeUiCoordinator(TextView status) {
        this.status = Objects.requireNonNull(
                status,
                "Realtime status view is required.");
    }

    void start(
            AttendanceFeatureRuntime runtime,
            Runnable invalidate) {
        stop();
        long expectedGeneration = generation;
        Runnable requiredInvalidation = Objects.requireNonNull(
                invalidate,
                "Realtime invalidation is required.");
        observation = Optional.of(
                Objects.requireNonNull(
                        runtime,
                        "Attendance runtime is required.")
                        .realtime()
                        .observe(new AttendanceRealtimeCoordinator.Listener() {
                            @Override
                            public void onRealtimeStateChanged(
                                    AttendanceRealtimeState state) {
                                if (generation == expectedGeneration) {
                                    render(state);
                                }
                            }

                            @Override
                            public void onAttendanceInvalidated() {
                                if (generation == expectedGeneration) {
                                    requiredInvalidation.run();
                                }
                            }
                        }));
    }

    void stop() {
        generation++;
        observation.ifPresent(
                AttendanceRealtimeSubscription::close);
        observation = Optional.empty();
        status.setVisibility(View.GONE);
    }

    private void render(AttendanceRealtimeState state) {
        int label = switch (state.phase()) {
            case CONNECTING ->
                    R.string.attendance_realtime_connecting;
            case CONNECTED ->
                    R.string.attendance_realtime_connected;
            case FALLBACK ->
                    R.string.attendance_realtime_fallback;
            case IDLE, CLOSED -> 0;
        };
        status.setVisibility(label == 0 ? View.GONE : View.VISIBLE);
        if (label != 0) {
            status.setText(label);
        }
    }
}
