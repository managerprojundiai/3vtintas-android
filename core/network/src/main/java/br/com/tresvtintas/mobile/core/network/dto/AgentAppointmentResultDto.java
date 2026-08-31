package br.com.tresvtintas.mobile.core.network.dto;

import java.util.Set;

public record AgentAppointmentResultDto(
        long appointmentId,
        String status,
        int revision,
        boolean changed) {
    private static final Set<String> STATUSES = Set.of(
            "scheduled",
            "confirmed",
            "completed",
            "cancelled");

    public AgentAppointmentResultDto {
        DtoValidation.requirePositive(
                appointmentId,
                "Agent appointment result ID");
        DtoValidation.requirePositive(
                revision,
                "Agent appointment result revision");
        if (!STATUSES.contains(status)) {
            throw new IllegalArgumentException(
                    "Agent appointment result status is invalid.");
        }
    }
}
