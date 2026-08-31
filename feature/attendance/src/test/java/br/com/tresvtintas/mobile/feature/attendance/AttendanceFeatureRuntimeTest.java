package br.com.tresvtintas.mobile.feature.attendance;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.core.attendance.AttendanceFailureKind;
import br.com.tresvtintas.mobile.core.attendance.AttendancePage;
import br.com.tresvtintas.mobile.core.attendance.AttendanceRealtimeCoordinator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.Test;

public final class AttendanceFeatureRuntimeTest {
    @Test
    public void assignedOnlyScopeRemainsExplicit() {
        ScheduledExecutorService scheduler =
                Executors.newSingleThreadScheduledExecutor();
        AttendanceRealtimeCoordinator realtime =
                realtime(scheduler);
        AttendanceFeatureRuntime runtime =
                new AttendanceFeatureRuntime(
                        (query, cursor) -> new AttendancePage(
                                List.of(),
                                Optional.empty()),
                        realtime,
                        Runnable::run,
                        OptionalLong.of(7),
                        true,
                        false,
                        true);

        assertTrue(
                "Assigned-only scope must be retained explicitly.",
                runtime.assignedOnly());
        assertFalse(
                "Reply permission must remain independent from read scope.",
                runtime.canReply());
        assertTrue(
                "Management permission must remain independent.",
                runtime.canManage());
        realtime.close();
        scheduler.shutdownNow();
    }

    @Test
    public void invalidOrganizationFailsClosed() {
        ScheduledExecutorService scheduler =
                Executors.newSingleThreadScheduledExecutor();
        AttendanceRealtimeCoordinator realtime =
                realtime(scheduler);
        assertThrows(
                IllegalArgumentException.class,
                () -> new AttendanceFeatureRuntime(
                        (query, cursor) -> new AttendancePage(
                                List.of(),
                                Optional.empty()),
                        realtime,
                        Runnable::run,
                        OptionalLong.of(0),
                        false,
                        false,
                        false));
        realtime.close();
        scheduler.shutdownNow();
    }

    @Test
    public void onlyRecoverableFailuresOfferRetry() {
        assertTrue(
                "Network failures must allow an explicit retry.",
                AttendanceText.retryable(
                        AttendanceFailureKind.NETWORK));
        assertFalse(
                "Revoked access must not offer a futile retry.",
                AttendanceText.retryable(
                        AttendanceFailureKind.ACCESS_REVOKED));
    }

    private static AttendanceRealtimeCoordinator realtime(
            ScheduledExecutorService scheduler) {
        return new AttendanceRealtimeCoordinator(
                (cursor, listener) -> () -> {
                },
                scheduler,
                Runnable::run);
    }
}
