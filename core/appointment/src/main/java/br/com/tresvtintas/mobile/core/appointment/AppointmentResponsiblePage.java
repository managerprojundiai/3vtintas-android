package br.com.tresvtintas.mobile.core.appointment;

import java.util.List;
import java.util.Optional;

public record AppointmentResponsiblePage(
        List<AppointmentPerson> items,
        Optional<String> nextCursor) {
    public AppointmentResponsiblePage {
        nextCursor = nextCursor == null ? Optional.empty() : nextCursor;
        if (items == null
                || items.size() > 100
                || items.stream().anyMatch(java.util.Objects::isNull)
                || nextCursor.filter(String::isBlank).isPresent()) {
            throw new IllegalArgumentException(
                    "Appointment responsible page is invalid.");
        }
        items = List.copyOf(items);
    }
}
