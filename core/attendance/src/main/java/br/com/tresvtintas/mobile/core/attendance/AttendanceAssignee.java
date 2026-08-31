package br.com.tresvtintas.mobile.core.attendance;

public record AttendanceAssignee(long id, String displayName) {
    public AttendanceAssignee {
        if (id < 1
                || displayName == null
                || displayName.isBlank()
                || displayName.length() > 200) {
            throw new IllegalArgumentException(
                    "Attendance assignee is invalid.");
        }
    }
}
