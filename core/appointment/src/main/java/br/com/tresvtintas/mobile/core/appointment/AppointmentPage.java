package br.com.tresvtintas.mobile.core.appointment;

import java.util.List;
import java.util.Optional;

public record AppointmentPage(
        List<AppointmentSummary> items,
        AppointmentOverview overview,
        Optional<String> nextCursor) {
    public AppointmentPage {
        nextCursor = nextCursor == null ? Optional.empty() : nextCursor;
        if (items == null
                || items.size() > 100
                || items.stream().anyMatch(java.util.Objects::isNull)
                || overview == null
                || nextCursor.filter(String::isBlank).isPresent()) {
            throw new IllegalArgumentException(
                    "Appointment page is invalid.");
        }
        items = List.copyOf(items);
    }
}
