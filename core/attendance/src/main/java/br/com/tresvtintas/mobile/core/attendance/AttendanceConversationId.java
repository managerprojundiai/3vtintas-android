package br.com.tresvtintas.mobile.core.attendance;

import java.util.regex.Pattern;

final class AttendanceConversationId {
    private static final Pattern PATTERN = Pattern.compile(
            "^(?:whatsapp:[1-9]\\d*"
                    + "|site_chat:[^/\\x00-\\x1f\\x7f]{1,160})$");

    private AttendanceConversationId() {
        throw new AssertionError("No instances.");
    }

    static String require(String value) {
        if (value == null || !PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Attendance conversation ID is invalid.");
        }
        return value;
    }
}
