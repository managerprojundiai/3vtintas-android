package br.com.tresvtintas.mobile.core.network.dto;

import java.util.List;

public record AppointmentDetailDto(
        long id,
        String kind,
        String status,
        String title,
        String scheduledAt,
        int durationMinutes,
        String location,
        AppointmentPersonDto responsible,
        AppointmentSummaryDto.Organization organization,
        AppointmentSummaryDto.Customer customer,
        AppointmentSummaryDto.Order order,
        long revision,
        List<String> allowedActions,
        String createdAt,
        String updatedAt,
        String description) {
    public AppointmentDetailDto {
        new AppointmentSummaryDto(
                id,
                kind,
                status,
                title,
                scheduledAt,
                durationMinutes,
                location,
                responsible,
                organization,
                customer,
                order,
                revision,
                allowedActions,
                createdAt,
                updatedAt);
        allowedActions = List.copyOf(allowedActions);
        description = DtoValidation.optionalText(
                description,
                "Appointment description",
                20_000);
    }
}
