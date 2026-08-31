package br.com.tresvtintas.mobile.core.network.dto;

import java.util.Set;

public record AgentAppointmentSummaryDto(
        Long appointmentId,
        String operation,
        String title,
        String kind,
        String responsibleName,
        String organizationName,
        String customerName,
        Integer expectedRevision,
        AgentAppointmentSnapshotDto before,
        AgentAppointmentSnapshotDto after,
        boolean accessAndAvailabilityWillBeRevalidated) {
    private static final Set<String> KINDS =
            Set.of("general", "collection");
    private static final String CREATE = "create";
    private static final String RESCHEDULE = "reschedule";

    public AgentAppointmentSummaryDto {
        appointmentId = DtoValidation.optionalPositive(
                appointmentId,
                "Agent appointment ID");
        title = DtoValidation.requireText(
                title,
                "Agent appointment title",
                200);
        responsibleName = DtoValidation.requireText(
                responsibleName,
                "Agent appointment responsible",
                200);
        organizationName = DtoValidation.optionalText(
                organizationName,
                "Agent appointment organization",
                200);
        customerName = DtoValidation.optionalText(
                customerName,
                "Agent appointment customer",
                500);
        if (expectedRevision != null && expectedRevision < 1) {
            throw new IllegalArgumentException(
                    "Agent appointment revision is invalid.");
        }
        if (!KINDS.contains(kind)
                || after == null
                || !accessAndAvailabilityWillBeRevalidated
                || !validOperation(
                        operation,
                        appointmentId,
                        expectedRevision,
                        before,
                        after)) {
            throw new IllegalArgumentException(
                    "Agent appointment summary is invalid.");
        }
    }

    private static boolean validOperation(
            String operation,
            Long appointmentId,
            Integer expectedRevision,
            AgentAppointmentSnapshotDto before,
            AgentAppointmentSnapshotDto after) {
        if (CREATE.equals(operation)) {
            return appointmentId == null
                    && expectedRevision == null
                    && before == null
                    && "scheduled".equals(after.status());
        }
        if (appointmentId == null
                || expectedRevision == null
                || before == null
                || Set.of("completed", "cancelled")
                        .contains(before.status())) {
            return false;
        }
        if (RESCHEDULE.equals(operation)) {
            return before.status().equals(after.status())
                    && java.util.Objects.equals(
                            before.location(),
                            after.location())
                    && (!before.scheduledAt().equals(after.scheduledAt())
                            || before.durationMinutes()
                                    != after.durationMinutes());
        }
        return "cancel".equals(operation)
                && "cancelled".equals(after.status())
                && before.scheduledAt().equals(after.scheduledAt())
                && before.durationMinutes() == after.durationMinutes()
                && java.util.Objects.equals(
                        before.location(),
                        after.location());
    }
}
