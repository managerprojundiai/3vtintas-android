package br.com.tresvtintas.mobile.core.network.dto;

import java.util.Set;

public record AppointmentUpdateRequest(
        String scope,
        long expectedRevision,
        String title,
        String description,
        String scheduledAt,
        int durationMinutes,
        String location,
        Long customerId,
        String kind,
        String confirmation) {
    public AppointmentUpdateRequest {
        scope = AppointmentCreateRequest.scope(scope);
        title = DtoValidation.requireText(title, "Appointment title", 200);
        description = DtoValidation.optionalText(
                description,
                "Appointment description",
                20_000);
        scheduledAt = DtoValidation.requireInstant(
                scheduledAt,
                "Appointment schedule");
        location = DtoValidation.optionalText(
                location,
                "Appointment location",
                5_000);
        customerId = DtoValidation.optionalPositive(
                customerId,
                "Appointment customer ID");
        kind = DtoValidation.requireText(kind, "Appointment kind", 20);
        if (expectedRevision < 1
                || durationMinutes < 1
                || durationMinutes > 1_440
                || !Set.of("general", "collection").contains(kind)
                || !"UPDATE_APPOINTMENT".equals(confirmation)) {
            throw new IllegalArgumentException(
                    "Appointment update is invalid.");
        }
    }
}
