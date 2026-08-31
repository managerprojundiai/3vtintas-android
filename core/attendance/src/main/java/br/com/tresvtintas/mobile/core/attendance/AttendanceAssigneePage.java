package br.com.tresvtintas.mobile.core.attendance;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record AttendanceAssigneePage(
        List<AttendanceAssignee> items,
        Optional<String> nextCursor) {
    public AttendanceAssigneePage {
        if (items == null
                || items.size() > 100
                || items.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "Attendance assignee page is invalid.");
        }
        items = List.copyOf(items);
        nextCursor = Objects.requireNonNull(
                nextCursor,
                "Attendance assignee cursor is required.");
        if (nextCursor.filter(value ->
                        !value.matches("^[A-Za-z0-9_-]{1,512}$"))
                .isPresent()) {
            throw new IllegalArgumentException(
                    "Attendance assignee cursor is invalid.");
        }
    }
}
