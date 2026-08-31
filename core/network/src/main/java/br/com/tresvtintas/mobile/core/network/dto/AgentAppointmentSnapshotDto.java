package br.com.tresvtintas.mobile.core.network.dto;

import java.util.Set;

public record AgentAppointmentSnapshotDto(
        String status,
        String scheduledAt,
        int durationMinutes,
        String location) {
    private static final Set<String> STATUSES = Set.of(
            "scheduled",
            "confirmed",
            "completed",
            "cancelled");

    public AgentAppointmentSnapshotDto {
        scheduledAt = DtoValidation.requireInstant(
                scheduledAt,
                "Agent appointment schedule");
        location = DtoValidation.optionalText(
                location,
                "Agent appointment location",
                5_000);
        if (!STATUSES.contains(status)
                || durationMinutes < 1
                || durationMinutes > 1_440) {
            throw new IllegalArgumentException(
                    "Agent appointment snapshot is invalid.");
        }
    }
}
