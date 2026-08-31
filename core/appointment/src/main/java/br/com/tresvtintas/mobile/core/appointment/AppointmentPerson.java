package br.com.tresvtintas.mobile.core.appointment;

import br.com.tresvtintas.mobile.core.model.AppRole;
import java.util.Optional;

public record AppointmentPerson(
        long id,
        Optional<String> name,
        AppRole role) {
    public AppointmentPerson {
        name = name == null ? Optional.empty() : name;
        if (id < 1
                || role == null
                || name.filter(String::isBlank).isPresent()) {
            throw new IllegalArgumentException(
                    "Appointment person is invalid.");
        }
    }
}
