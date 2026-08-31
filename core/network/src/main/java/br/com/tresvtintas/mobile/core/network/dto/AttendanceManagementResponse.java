package br.com.tresvtintas.mobile.core.network.dto;

import java.util.Set;

public record AttendanceManagementResponse(
        String conversationId,
        int revision,
        String folder,
        String priority,
        AttendanceConversationDto.AssignedUser assignedUser,
        String updatedAt) {
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

    public AttendanceManagementResponse {
        conversationId = DtoValidation.requireText(
                conversationId,
                "Attendance conversation ID",
                200);
        updatedAt = DtoValidation.requireInstant(
                updatedAt,
                "Attendance management update");
        if (revision < 1
                || !FOLDERS.contains(folder)
                || !PRIORITIES.contains(priority)) {
            throw new IllegalArgumentException(
                    "Attendance management response is invalid.");
        }
    }
}
