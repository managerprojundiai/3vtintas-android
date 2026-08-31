package br.com.tresvtintas.mobile.core.agent;

import br.com.tresvtintas.mobile.core.appointment.AppointmentStatus;
import java.util.Objects;

public record AgentAppointmentResult(
        long appointmentId,
        AppointmentStatus status,
        int revision,
        boolean changed) implements AgentActionResult {
    public AgentAppointmentResult {
        status = Objects.requireNonNull(
                status,
                "Agent appointment result status is required.");
        if (appointmentId < 1 || revision < 1) {
            throw new IllegalArgumentException(
                    "Agent appointment result is invalid.");
        }
    }
}
