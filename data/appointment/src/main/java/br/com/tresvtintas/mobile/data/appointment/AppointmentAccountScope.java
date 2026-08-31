package br.com.tresvtintas.mobile.data.appointment;

import br.com.tresvtintas.mobile.core.appointment.AppointmentScope;
import java.util.Objects;

public record AppointmentAccountScope(
        long userId,
        String authorizationRevision,
        AppointmentScope scope) {
    private static final long MINIMUM_USER_ID = 1L;

    public AppointmentAccountScope {
        if (userId < MINIMUM_USER_ID) {
            throw new IllegalArgumentException(
                    "Appointment scope user ID is invalid.");
        }
        authorizationRevision = Objects.requireNonNull(
                authorizationRevision,
                "Appointment authorization revision is required.");
        if (!authorizationRevision.matches("^[0-9a-f]{64}$")
                || scope == null) {
            throw new IllegalArgumentException(
                    "Appointment account scope is invalid.");
        }
    }
}
