package br.com.tresvtintas.mobile.feature.attendance;

import java.util.Optional;

@FunctionalInterface
public interface AttendanceRuntimeProvider {
    Optional<AttendanceFeatureRuntime> attendanceRuntime();
}
