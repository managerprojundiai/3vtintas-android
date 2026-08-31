package br.com.tresvtintas.mobile.feature.agent;

import android.content.Context;
import br.com.tresvtintas.mobile.core.agent.AgentAppointmentOperation;
import br.com.tresvtintas.mobile.core.agent.AgentAppointmentSnapshot;
import br.com.tresvtintas.mobile.core.agent.AgentAppointmentSummary;
import br.com.tresvtintas.mobile.core.appointment.AppointmentKind;
import br.com.tresvtintas.mobile.core.appointment.AppointmentStatus;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class AgentAppointmentText {
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern(
                            "dd/MM/yyyy 'às' HH:mm",
                            new Locale("pt", "BR"))
                    .withZone(ZoneId.systemDefault());

    private AgentAppointmentText() {
        throw new AssertionError("No instances.");
    }

    static String compact(
            Context context,
            AgentAppointmentSummary summary) {
        return context.getString(
                R.string.agent_action_appointment_summary,
                operation(context, summary.operation()),
                kind(context, summary.kind()),
                DATE_TIME.format(summary.after().scheduledAt()),
                summary.responsibleName());
    }

    static String operation(
            Context context,
            AgentAppointmentOperation operation) {
        return context.getString(switch (operation) {
            case CREATE ->
                    R.string.agent_action_appointment_operation_create;
            case RESCHEDULE ->
                    R.string.agent_action_appointment_operation_reschedule;
            case CANCEL ->
                    R.string.agent_action_appointment_operation_cancel;
        });
    }

    static String kind(
            Context context,
            AppointmentKind kind) {
        return context.getString(switch (kind) {
            case GENERAL ->
                    R.string.agent_action_appointment_kind_general;
            case COLLECTION ->
                    R.string.agent_action_appointment_kind_collection;
            case DELIVERY -> throw new IllegalArgumentException(
                    "Delivery appointments cannot be changed by the agent.");
        });
    }

    static String snapshot(
            Context context,
            AgentAppointmentSnapshot snapshot) {
        return context.getString(
                R.string.agent_action_appointment_snapshot,
                status(context, snapshot.status()),
                DATE_TIME.format(snapshot.scheduledAt()),
                snapshot.durationMinutes(),
                snapshot.location().orElseGet(() -> context.getString(
                        R.string.agent_action_appointment_no_location)));
    }

    private static String status(
            Context context,
            AppointmentStatus status) {
        return context.getString(switch (status) {
            case SCHEDULED ->
                    R.string.agent_action_appointment_status_scheduled;
            case CONFIRMED ->
                    R.string.agent_action_appointment_status_confirmed;
            case COMPLETED ->
                    R.string.agent_action_appointment_status_completed;
            case CANCELLED ->
                    R.string.agent_action_appointment_status_cancelled;
        });
    }
}
