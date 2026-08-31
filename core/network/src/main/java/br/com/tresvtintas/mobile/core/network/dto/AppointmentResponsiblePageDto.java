package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;

public record AppointmentResponsiblePageDto(
        List<AppointmentPersonDto> items,
        String nextCursor) {
    public AppointmentResponsiblePageDto {
        if (items == null
                || items.size() > 100
                || items.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException(
                    "Appointment responsible page is invalid.");
        }
        items = List.copyOf(items);
        nextCursor = DtoValidation.optionalText(
                nextCursor,
                "Appointment responsible cursor",
                160);
    }
}
