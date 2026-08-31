package br.com.tresvtintas.mobile.core.attendance;

import java.util.regex.Pattern;

public final class AttendanceReplyContent {
    public static final int MAXIMUM_LENGTH = 3_500;
    private static final Pattern FORBIDDEN_CONTROL = Pattern.compile(
            "[\\x00-\\x08\\x0b\\x0c\\x0e-\\x1f\\x7f]");

    private AttendanceReplyContent() {
        throw new AssertionError("No instances.");
    }

    public static String normalize(String value) {
        if (value == null) {
            throw invalid();
        }
        String normalized = value
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
        if (normalized.isEmpty()
                || normalized.length() > MAXIMUM_LENGTH
                || FORBIDDEN_CONTROL.matcher(normalized).find()) {
            throw invalid();
        }
        return normalized;
    }

    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException(
                "Attendance reply content is invalid.");
    }
}
