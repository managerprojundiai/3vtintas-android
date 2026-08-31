package br.com.tresvtintas.mobile.core.attendance;

final class AttendanceIdempotencyKey {
    private AttendanceIdempotencyKey() {
        throw new AssertionError("No instances.");
    }

    static String require(String value) {
        if (value == null
                || value.length() < 16
                || value.length() > 255
                || !value.matches("^[\\x21-\\x7e]+$")) {
            throw new IllegalArgumentException(
                    "Attendance idempotency key is invalid.");
        }
        return value;
    }
}
