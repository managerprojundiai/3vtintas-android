package br.com.tresvtintas.mobile.core.appointment;

import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;

public record AppointmentResponsibleQuery(
        AppointmentScope scope,
        OptionalLong organizationId,
        Optional<String> search,
        int pageSize) {
    public AppointmentResponsibleQuery {
        organizationId = organizationId == null
                ? OptionalLong.empty()
                : organizationId;
        search = search == null ? Optional.empty() : search;
        search = search.map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.toLowerCase(Locale.ROOT));
        if (scope == null
                || (organizationId.isPresent()
                        && organizationId.orElseThrow() < 1)
                || search.map(String::length).orElse(0) > 100
                || pageSize < 1
                || pageSize > 100) {
            throw new IllegalArgumentException(
                    "Appointment responsible query is invalid.");
        }
    }
}
