package br.com.tresvtintas.mobile.core.network.dto;

public record AppointmentOverviewDto(
        long scheduled,
        long confirmed,
        long completed,
        long cancelled) {
    public AppointmentOverviewDto {
        if (scheduled < 0 || confirmed < 0 || completed < 0 || cancelled < 0) {
            throw new IllegalArgumentException(
                    "Appointment overview is invalid.");
        }
    }
}
