package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;
import java.util.Objects;

public record AttendanceMessagePageDto(
        String conversationId,
        List<AttendanceMessageDto> items,
        String nextCursor) {
    public AttendanceMessagePageDto {
        conversationId = DtoValidation.requireText(
                conversationId,
                "Attendance conversation ID",
                200);
        if (items == null
                || items.size() > 100
                || items.stream().anyMatch(Objects::isNull)
                || (nextCursor != null
                        && !nextCursor.matches(
                                "^[A-Za-z0-9_-]{1,512}$"))) {
            throw new IllegalArgumentException(
                    "Attendance message page is invalid.");
        }
        items = List.copyOf(items);
    }
}
