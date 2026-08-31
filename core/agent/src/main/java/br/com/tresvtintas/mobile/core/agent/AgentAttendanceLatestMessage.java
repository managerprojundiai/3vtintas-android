package br.com.tresvtintas.mobile.core.agent;

import java.time.Instant;
import java.util.Objects;

public record AgentAttendanceLatestMessage(
        AgentAttendanceMessageDirection direction,
        String preview,
        Instant createdAt) {
    private static final int MAXIMUM_PREVIEW_LENGTH = 240;

    public AgentAttendanceLatestMessage {
        direction = Objects.requireNonNull(
                direction,
                "Attendance message direction is required.");
        preview = Objects.requireNonNull(
                preview,
                "Attendance message preview is required.");
        if (preview.length() > MAXIMUM_PREVIEW_LENGTH) {
            throw new IllegalArgumentException(
                    "Attendance message preview is invalid.");
        }
        createdAt = Objects.requireNonNull(
                createdAt,
                "Attendance message timestamp is required.");
    }
}
