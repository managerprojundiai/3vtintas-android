package br.com.tresvtintas.mobile.core.appointment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record AppointmentSnapshot(
        List<AppointmentSummary> items,
        AppointmentOverview overview,
        Optional<String> nextCursor) {
    public AppointmentSnapshot {
        nextCursor = nextCursor == null ? Optional.empty() : nextCursor;
        if (items == null
                || items.stream().anyMatch(java.util.Objects::isNull)
                || overview == null
                || nextCursor.filter(String::isBlank).isPresent()) {
            throw new IllegalArgumentException(
                    "Appointment snapshot is invalid.");
        }
        items = List.copyOf(items);
    }

    public static AppointmentSnapshot from(AppointmentPage page) {
        return new AppointmentSnapshot(
                page.items(),
                page.overview(),
                page.nextCursor());
    }

    public AppointmentSnapshot append(AppointmentPage page) {
        Map<Long, AppointmentSummary> unique = new LinkedHashMap<>();
        new ArrayList<>(items).forEach(item -> unique.put(item.id(), item));
        page.items().forEach(item -> unique.put(item.id(), item));
        return new AppointmentSnapshot(
                List.copyOf(unique.values()),
                page.overview(),
                page.nextCursor());
    }

    public boolean hasMore() {
        return nextCursor.isPresent();
    }
}
