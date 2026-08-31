package br.com.tresvtintas.mobile.core.agent;

import br.com.tresvtintas.mobile.core.appointment.AppointmentKind;
import br.com.tresvtintas.mobile.core.appointment.AppointmentStatus;
import java.util.Objects;
import java.util.Optional;

public record AgentAppointmentSummary(
        Optional<Long> appointmentId,
        AgentAppointmentOperation operation,
        String title,
        AppointmentKind kind,
        String responsibleName,
        Optional<String> organizationName,
        Optional<String> customerName,
        Optional<Integer> expectedRevision,
        Optional<AgentAppointmentSnapshot> before,
        AgentAppointmentSnapshot after,
        boolean accessAndAvailabilityWillBeRevalidated)
        implements AgentActionSummary {
    public AgentAppointmentSummary {
        appointmentId = appointmentId == null
                ? Optional.empty()
                : appointmentId;
        operation = Objects.requireNonNull(
                operation,
                "Agent appointment operation is required.");
        title = requireText(
                title,
                200,
                "Agent appointment title is invalid.");
        kind = Objects.requireNonNull(
                kind,
                "Agent appointment kind is required.");
        responsibleName = requireText(
                responsibleName,
                200,
                "Agent appointment responsible is invalid.");
        organizationName = optionalText(organizationName, 200);
        customerName = optionalText(customerName, 500);
        expectedRevision = expectedRevision == null
                ? Optional.empty()
                : expectedRevision;
        before = before == null ? Optional.empty() : before;
        after = Objects.requireNonNull(
                after,
                "Agent appointment next state is required.");
        if (kind == AppointmentKind.DELIVERY
                || appointmentId.filter(value -> value < 1).isPresent()
                || expectedRevision.filter(value -> value < 1).isPresent()
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
            AgentAppointmentOperation operation,
            Optional<Long> appointmentId,
            Optional<Integer> expectedRevision,
            Optional<AgentAppointmentSnapshot> before,
            AgentAppointmentSnapshot after) {
        if (operation == AgentAppointmentOperation.CREATE) {
            return appointmentId.isEmpty()
                    && expectedRevision.isEmpty()
                    && before.isEmpty()
                    && after.status() == AppointmentStatus.SCHEDULED;
        }
        if (appointmentId.isEmpty()
                || expectedRevision.isEmpty()
                || before.isEmpty()
                || before.get().status().isTerminal()) {
            return false;
        }
        AgentAppointmentSnapshot previous = before.get();
        if (operation == AgentAppointmentOperation.RESCHEDULE) {
            return previous.status() == after.status()
                    && previous.location().equals(after.location())
                    && (!previous.scheduledAt().equals(after.scheduledAt())
                            || previous.durationMinutes()
                                    != after.durationMinutes());
        }
        return after.status() == AppointmentStatus.CANCELLED
                && previous.scheduledAt().equals(after.scheduledAt())
                && previous.durationMinutes() == after.durationMinutes()
                && previous.location().equals(after.location());
    }

    private static Optional<String> optionalText(
            Optional<String> value,
            int maximum) {
        Optional<String> normalized = value == null
                ? Optional.empty()
                : value;
        return normalized.map(text ->
                requireText(
                        text,
                        maximum,
                        "Agent appointment text is invalid."));
    }

    private static String requireText(
            String value,
            int maximum,
            String message) {
        if (value == null
                || value.isBlank()
                || value.length() > maximum
                || value.matches(
                        ".*[\\u0000-\\u0008\\u000b\\u000c\\u000e-\\u001f\\u007f].*")) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
