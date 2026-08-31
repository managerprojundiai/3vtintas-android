package br.com.tresvtintas.mobile.core.attendance;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record AttendanceManagementResult(
        String conversationId,
        int revision,
        AttendanceFolder folder,
        AttendancePriority priority,
        Optional<AttendanceConversation.AssignedUser> assignedUser,
        Instant updatedAt,
        boolean replayed) {
    private static final int MINIMUM_REVISION = 1;

    public AttendanceManagementResult {
        conversationId = AttendanceConversationId.require(conversationId);
        if (revision < MINIMUM_REVISION) {
            throw new IllegalArgumentException(
                    "Attendance management revision is invalid.");
        }
        Objects.requireNonNull(folder, "Attendance folder is required.");
        Objects.requireNonNull(priority, "Attendance priority is required.");
        assignedUser = Objects.requireNonNull(
                assignedUser,
                "Attendance assigned user is required.");
        Objects.requireNonNull(
                updatedAt,
                "Attendance management update is required.");
    }

    public AttendanceManagementSelection selection() {
        return new AttendanceManagementSelection(
                folder,
                priority,
                assignedUser
                        .map(value -> java.util.OptionalLong.of(value.id()))
                        .orElseGet(java.util.OptionalLong::empty));
    }
}
