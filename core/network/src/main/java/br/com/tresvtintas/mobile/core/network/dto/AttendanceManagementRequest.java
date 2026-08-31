package br.com.tresvtintas.mobile.core.network.dto;

import java.util.Set;

public record AttendanceManagementRequest(
        int expectedRevision,
        String folder,
        String priority,
        Long assignedToUserId,
        String confirmation) {
    private static final Set<String> FOLDERS = Set.of(
            "inbox",
            "mine",
            "unassigned",
            "urgent",
            "follow_up",
            "fornecedores",
            "vip",
            "resolved");
    private static final Set<String> PRIORITIES =
            Set.of("low", "normal", "high", "urgent");

    public AttendanceManagementRequest {
        assignedToUserId = DtoValidation.optionalPositive(
                assignedToUserId,
                "Attendance assigned user ID");
        if (expectedRevision < 1
                || !FOLDERS.contains(folder)
                || !PRIORITIES.contains(priority)
                || !"UPDATE_ATTENDANCE_CONVERSATION".equals(
                        confirmation)) {
            throw new IllegalArgumentException(
                    "Attendance management request is invalid.");
        }
    }
}
