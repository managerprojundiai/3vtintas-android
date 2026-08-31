package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;
import java.util.Objects;

public record AttendanceAssigneePageDto(
        List<AttendanceAssigneeDto> items,
        String nextCursor) {
    public AttendanceAssigneePageDto {
        if (items == null
                || items.size() > 100
                || items.stream().anyMatch(Objects::isNull)
                || (nextCursor != null
                        && !nextCursor.matches(
                                "^[A-Za-z0-9_-]{1,512}$"))) {
            throw new IllegalArgumentException(
                    "Attendance assignee page is invalid.");
        }
        items = List.copyOf(items);
    }
}
