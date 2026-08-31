package br.com.tresvtintas.mobile.core.attendance;

import java.util.Objects;
import java.util.OptionalLong;

public record AttendanceManagementSelection(
        AttendanceFolder folder,
        AttendancePriority priority,
        OptionalLong assignedToUserId) {
    public AttendanceManagementSelection {
        Objects.requireNonNull(folder, "Attendance folder is required.");
        Objects.requireNonNull(priority, "Attendance priority is required.");
        assignedToUserId = Objects.requireNonNull(
                assignedToUserId,
                "Attendance assignment is required.");
        if (assignedToUserId.isPresent()
                && assignedToUserId.getAsLong() < 1) {
            throw new IllegalArgumentException(
                    "Attendance assignment is invalid.");
        }
    }

    public static AttendanceManagementSelection from(
            AttendanceConversation conversation) {
        Objects.requireNonNull(
                conversation,
                "Attendance conversation is required.");
        OptionalLong assigned = conversation.assignedUser()
                .map(value -> OptionalLong.of(value.id()))
                .orElseGet(OptionalLong::empty);
        return new AttendanceManagementSelection(
                conversation.folder(),
                conversation.priority(),
                assigned);
    }
}
