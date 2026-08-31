package br.com.tresvtintas.mobile.core.network.dto;

import java.util.Set;

public record AppointmentTransitionRequest(
        String scope,
        long expectedRevision,
        String status,
        String confirmation) {
    public AppointmentTransitionRequest {
        scope = AppointmentCreateRequest.scope(scope);
        status = DtoValidation.requireText(
                status,
                "Appointment status",
                20);
        if (expectedRevision < 1
                || !Set.of("confirmed", "completed", "cancelled")
                        .contains(status)
                || !"TRANSITION_APPOINTMENT".equals(confirmation)) {
            throw new IllegalArgumentException(
                    "Appointment transition is invalid.");
        }
    }
}
