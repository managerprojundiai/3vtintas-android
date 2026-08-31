package br.com.tresvtintas.mobile.core.attendance;

import java.time.Instant;
import java.util.Objects;

public record AttendanceRealtimeEvent(
        Kind kind,
        String cursor,
        Instant occurredAt) {
    public enum Kind {
        READY,
        CHANGE,
        CHECKPOINT
    }

    public AttendanceRealtimeEvent {
        Objects.requireNonNull(kind, "Realtime event kind is required.");
        if (cursor == null
                || !cursor.matches("^\\d{1,16}$")
                || !safeCursor(cursor)) {
            throw new IllegalArgumentException(
                    "Realtime event cursor is invalid.");
        }
        Objects.requireNonNull(
                occurredAt,
                "Realtime event timestamp is required.");
    }

    public long cursorValue() {
        return Long.parseLong(cursor);
    }

    private static boolean safeCursor(String value) {
        try {
            return Long.parseLong(value) >= 0;
        } catch (NumberFormatException failure) {
            return false;
        }
    }
}
