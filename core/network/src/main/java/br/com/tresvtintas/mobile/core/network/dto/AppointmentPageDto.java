package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;

public record AppointmentPageDto(
        List<AppointmentSummaryDto> items,
        AppointmentOverviewDto overview,
        String nextCursor) {
    public AppointmentPageDto {
        if (items == null
                || items.size() > 100
                || items.stream().anyMatch(java.util.Objects::isNull)
                || overview == null) {
            throw new IllegalArgumentException(
                    "Appointment page is invalid.");
        }
        items = List.copyOf(items);
        nextCursor = DtoValidation.optionalText(
                nextCursor,
                "Appointment cursor",
                160);
    }
}
