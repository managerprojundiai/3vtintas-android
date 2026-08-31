package br.com.tresvtintas.mobile.core.attendance;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record AttendancePage(
        List<AttendanceConversation> items,
        Optional<String> nextCursor) {
    public AttendancePage {
        if (items == null
                || items.size() > 100
                || items.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "Attendance page is invalid.");
        }
        items = List.copyOf(items);
        nextCursor = Objects.requireNonNull(
                nextCursor,
                "Attendance cursor is required.");
    }
}
