package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;
import java.util.Objects;

public record AttendancePageDto(
        List<AttendanceConversationDto> items,
        String nextCursor) {
    public AttendancePageDto {
        if (items == null
                || items.size() > 100
                || items.stream().anyMatch(Objects::isNull)
                || (nextCursor != null
                        && (!nextCursor.matches("^[A-Za-z0-9_-]{1,512}$")))) {
            throw new IllegalArgumentException(
                    "Attendance page is invalid.");
        }
        items = List.copyOf(items);
    }
}
