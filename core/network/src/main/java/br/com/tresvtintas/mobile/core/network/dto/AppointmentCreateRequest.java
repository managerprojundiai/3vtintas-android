package br.com.tresvtintas.mobile.core.network.dto;

import java.util.Set;

public record AppointmentCreateRequest(
        String scope,
        Long responsibleUserId,
        Long organizationId,
        String kind,
        String title,
        String description,
        String scheduledAt,
        int durationMinutes,
        String location,
        Long customerId,
        String confirmation) {
    public AppointmentCreateRequest {
        scope = scope(scope);
        responsibleUserId = DtoValidation.optionalPositive(
                responsibleUserId,
                "Appointment responsible ID");
        organizationId = DtoValidation.optionalPositive(
                organizationId,
                "Appointment organization ID");
        kind = DtoValidation.requireText(kind, "Appointment kind", 20);
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
        if (!Set.of("general", "collection").contains(kind)
                || durationMinutes < 1
                || durationMinutes > 1_440
                || ("self".equals(scope) && responsibleUserId != null)
                || (!"self".equals(scope) && responsibleUserId == null)
                || !"CREATE_APPOINTMENT".equals(confirmation)) {
            throw new IllegalArgumentException(
                    "Appointment creation is invalid.");
        }
    }

    static String scope(String value) {
        String scope = DtoValidation.requireText(
                value,
                "Appointment scope",
                20);
        if (!Set.of("self", "team", "all").contains(scope)) {
            throw new IllegalArgumentException(
                    "Appointment scope is invalid.");
        }
        return scope;
    }
}
