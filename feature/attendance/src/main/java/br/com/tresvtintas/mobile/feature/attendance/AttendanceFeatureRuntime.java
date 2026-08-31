package br.com.tresvtintas.mobile.feature.attendance;

import br.com.tresvtintas.mobile.core.attendance.AttendanceRepository;
import br.com.tresvtintas.mobile.core.attendance.AttendanceRealtimeCoordinator;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.concurrent.Executor;

public record AttendanceFeatureRuntime(
        AttendanceRepository repository,
        AttendanceRealtimeCoordinator realtime,
        Executor workerExecutor,
        OptionalLong organizationId,
        boolean assignedOnly,
        boolean canReply,
        boolean canManage) {
    public AttendanceFeatureRuntime {
        Objects.requireNonNull(
                repository,
                "Attendance repository is required.");
        Objects.requireNonNull(
                realtime,
                "Attendance realtime coordinator is required.");
        Objects.requireNonNull(
                workerExecutor,
                "Attendance worker is required.");
        organizationId = Objects.requireNonNull(
                organizationId,
                "Attendance organization is required.");
        if (organizationId.isPresent()
                && organizationId.getAsLong() < 1) {
            throw new IllegalArgumentException(
                    "Attendance organization is invalid.");
        }
    }
}
