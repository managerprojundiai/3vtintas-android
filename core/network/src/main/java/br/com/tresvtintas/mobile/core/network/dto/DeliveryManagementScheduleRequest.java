package br.com.tresvtintas.mobile.core.network.dto;

public record DeliveryManagementScheduleRequest(
        long organizationId,
        int expectedOrderRevision,
        String scheduledAt,
        int duration,
        String location,
        String notes,
        String title,
        String confirmation) {
    public DeliveryManagementScheduleRequest {
        organizationId = DtoValidation.requirePositive(
                organizationId,
                "Management organization ID");
        if (expectedOrderRevision < 1
                || duration < 1
                || duration > 1_440
                || !"SCHEDULE_DELIVERY".equals(confirmation)) {
            throw new IllegalArgumentException(
                    "Management schedule request is invalid.");
        }
        scheduledAt = DtoValidation.requireInstant(
                scheduledAt,
                "Management schedule");
        location = DtoValidation.optionalText(
                location,
                "Management location",
                5_000);
        notes = DtoValidation.optionalText(
                notes,
                "Management notes",
                10_000);
        title = DtoValidation.optionalText(
                title,
                "Management title",
                200);
    }

    public DeliveryManagementScheduleRequest(
            long organizationId,
            int expectedOrderRevision,
            String scheduledAt,
            int duration) {
        this(
                organizationId,
                expectedOrderRevision,
                scheduledAt,
                duration,
                null,
                null,
                null,
                "SCHEDULE_DELIVERY");
    }
}
