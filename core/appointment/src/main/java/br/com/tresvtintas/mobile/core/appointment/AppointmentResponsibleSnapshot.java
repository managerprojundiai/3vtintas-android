package br.com.tresvtintas.mobile.core.appointment;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record AppointmentResponsibleSnapshot(
        List<AppointmentPerson> items,
        Optional<String> nextCursor) {
    public AppointmentResponsibleSnapshot {
        nextCursor = nextCursor == null ? Optional.empty() : nextCursor;
        if (items == null
                || items.stream().anyMatch(java.util.Objects::isNull)
                || nextCursor.filter(String::isBlank).isPresent()) {
            throw new IllegalArgumentException(
                    "Appointment responsible snapshot is invalid.");
        }
        items = List.copyOf(items);
    }

    public static AppointmentResponsibleSnapshot from(
            AppointmentResponsiblePage page) {
        return new AppointmentResponsibleSnapshot(
                page.items(),
                page.nextCursor());
    }

    public AppointmentResponsibleSnapshot append(
            AppointmentResponsiblePage page) {
        Map<Long, AppointmentPerson> unique = new LinkedHashMap<>();
        items.forEach(item -> unique.put(item.id(), item));
        page.items().forEach(item -> unique.put(item.id(), item));
        return new AppointmentResponsibleSnapshot(
                List.copyOf(unique.values()),
                page.nextCursor());
    }
}
