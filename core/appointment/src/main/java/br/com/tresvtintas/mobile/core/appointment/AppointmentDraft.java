package br.com.tresvtintas.mobile.core.appointment;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

public record AppointmentDraft(
        AppointmentScope scope,
        OptionalLong responsibleUserId,
        OptionalLong organizationId,
        AppointmentKind kind,
        String title,
        Optional<String> description,
        Instant scheduledAt,
        int durationMinutes,
        Optional<String> location,
        OptionalLong customerId) {
    public AppointmentDraft {
        responsibleUserId = responsibleUserId == null
                ? OptionalLong.empty()
                : responsibleUserId;
        organizationId = organizationId == null
                ? OptionalLong.empty()
                : organizationId;
        description = normalize(description);
        location = normalize(location);
        customerId = customerId == null ? OptionalLong.empty() : customerId;
        title = title == null ? "" : title.trim();
        boolean targetMismatch = scope != null
                && ((scope == AppointmentScope.SELF
                                && responsibleUserId.isPresent())
                        || (scope != AppointmentScope.SELF
                                && responsibleUserId.isEmpty()));
        if (scope == null
                || targetMismatch
                || (responsibleUserId.isPresent()
                        && responsibleUserId.orElseThrow() < 1)
                || (organizationId.isPresent()
                        && organizationId.orElseThrow() < 1)
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
                    "Appointment draft is invalid.");
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
