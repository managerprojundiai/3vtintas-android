package br.com.tresvtintas.mobile.core.network.dto;

import java.util.Set;

public record AppointmentMutationResponse(
        long appointmentId,
        String status,
        long revision,
        boolean changed) {
    public AppointmentMutationResponse {
        appointmentId = DtoValidation.requirePositive(
                appointmentId,
                "Appointment ID");
        status = DtoValidation.requireText(
                status,
                "Appointment status",
                20);
        if (!Set.of("scheduled", "confirmed", "completed", "cancelled")
                        .contains(status)
                || revision < 1) {
            throw new IllegalArgumentException(
                    "Appointment mutation response is invalid.");
        }
    }
}
