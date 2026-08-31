package br.com.tresvtintas.mobile.core.attendance;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public record AttendanceMessagePage(
        String conversationId,
        List<AttendanceMessage> items,
        Optional<String> nextCursor) {
    private static final int MAXIMUM_CURSOR_LENGTH = 512;
    private static final Pattern CURSOR_PATTERN =
            Pattern.compile("^[A-Za-z0-9_-]+$");

    public AttendanceMessagePage {
        if (conversationId == null
                || conversationId.isBlank()
                || conversationId.length() > 200) {
            throw new IllegalArgumentException(
                    "Attendance conversation ID is invalid.");
        }
        if (items == null
                || items.size() > 100
                || items.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "Attendance message page is invalid.");
        }
        items = List.copyOf(items);
        nextCursor = Objects.requireNonNull(
                nextCursor,
                "Attendance message cursor is required.");
        if (nextCursor.filter(value -> value.isBlank()
                        || value.length() > MAXIMUM_CURSOR_LENGTH
                        || !CURSOR_PATTERN.matcher(value).matches())
                .isPresent()) {
            throw new IllegalArgumentException(
                    "Attendance message cursor is invalid.");
        }
    }
}
