package br.com.tresvtintas.mobile.core.appointment;

import java.util.Optional;

public record AppointmentDetail(
        AppointmentSummary summary,
        Optional<String> description) {
    public AppointmentDetail {
        description = description == null ? Optional.empty() : description;
        if (summary == null
                || description.filter(value -> value.length() > 20_000)
                        .isPresent()) {
            throw new IllegalArgumentException(
                    "Appointment detail is invalid.");
        }
    }
}
