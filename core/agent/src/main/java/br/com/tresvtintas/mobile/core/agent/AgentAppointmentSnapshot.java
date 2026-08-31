package br.com.tresvtintas.mobile.core.agent;

import br.com.tresvtintas.mobile.core.appointment.AppointmentStatus;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record AgentAppointmentSnapshot(
        AppointmentStatus status,
        Instant scheduledAt,
        int durationMinutes,
        Optional<String> location) {
    public AgentAppointmentSnapshot {
        status = Objects.requireNonNull(
                status,
                "Agent appointment status is required.");
        scheduledAt = Objects.requireNonNull(
                scheduledAt,
                "Agent appointment schedule is required.");
        location = location == null ? Optional.empty() : location;
        if (durationMinutes < 1
                || durationMinutes > 1_440
                || location.filter(value -> value.length() > 5_000)
                        .isPresent()) {
            throw new IllegalArgumentException(
                    "Agent appointment snapshot is invalid.");
        }
    }
}
