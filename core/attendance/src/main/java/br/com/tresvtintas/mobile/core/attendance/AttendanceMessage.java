package br.com.tresvtintas.mobile.core.attendance;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record AttendanceMessage(
        String id,
        String sourceId,
        AttendanceMessageDirection direction,
        String type,
        String content,
        boolean truncated,
        Optional<String> status,
        Instant createdAt) {
    private static final int MAXIMUM_STATUS_LENGTH = 80;

    public AttendanceMessage {
        id = required(id, "Attendance message ID", 400);
        sourceId = required(
                sourceId,
                "Attendance message source ID",
                160);
        if (!sourceId.matches("[1-9]\\d*")
                || new BigInteger(sourceId).compareTo(
                        BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
            throw new IllegalArgumentException(
                    "Attendance message source ID is invalid.");
        }
        Objects.requireNonNull(
                direction,
                "Attendance message direction is required.");
        type = required(type, "Attendance message type", 80);
        if (content == null || content.length() > 8_000) {
            throw new IllegalArgumentException(
                    "Attendance message content is invalid.");
        }
        status = Objects.requireNonNull(
                status,
                "Attendance message status is required.");
        if (status.filter(value -> value.isBlank()
                        || value.length() > MAXIMUM_STATUS_LENGTH)
                .isPresent()) {
            throw new IllegalArgumentException(
                    "Attendance message status is invalid.");
        }
        Objects.requireNonNull(
                createdAt,
                "Attendance message creation is required.");
    }

    private static String required(
            String value,
            String field,
            int maximumLength) {
        if (value == null
                || value.isBlank()
                || value.length() > maximumLength) {
            throw new IllegalArgumentException(field + " is invalid.");
        }
        return value;
    }
}
