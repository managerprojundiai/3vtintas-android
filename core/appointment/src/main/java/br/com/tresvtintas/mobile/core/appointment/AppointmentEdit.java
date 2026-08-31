package br.com.tresvtintas.mobile.core.appointment;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

public record AppointmentEdit(
        AppointmentScope scope,
        long expectedRevision,
        AppointmentKind kind,
        String title,
        Optional<String> description,
        Instant scheduledAt,
        int durationMinutes,
        Optional<String> location,
        OptionalLong customerId) {
    public AppointmentEdit {
        description = normalize(description);
        location = normalize(location);
        customerId = customerId == null ? OptionalLong.empty() : customerId;
        title = title == null ? "" : title.trim();
        if (scope == null
                || expectedRevision < 1
                || kind == null
                || !kind.isWritable()
                || title.isBlank()
                || title.length() > 200
                || description.map(String::length).orElse(0) > 20_000
                || scheduledAt == null
                || durationMinutes < 1
                || durationMinutes > 1_440
                || location.map(String::length).orElse(0) > 5_000
                || (customerId.isPresent()
                        && customerId.orElseThrow() < 1)) {
            throw new IllegalArgumentException(
                    "Appointment edit is invalid.");
        }
    }

    private static Optional<String> normalize(Optional<String> value) {
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }
        String normalized = value.orElseThrow().trim();
        return normalized.isEmpty()
                ? Optional.empty()
                : Optional.of(normalized);
    }
}
