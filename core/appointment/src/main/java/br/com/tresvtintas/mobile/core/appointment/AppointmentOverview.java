package br.com.tresvtintas.mobile.core.appointment;

public record AppointmentOverview(
        long scheduled,
        long confirmed,
        long completed,
        long cancelled) {
    public AppointmentOverview {
        if (scheduled < 0 || confirmed < 0 || completed < 0 || cancelled < 0) {
            throw new IllegalArgumentException(
                    "Appointment overview is invalid.");
        }
    }
}
