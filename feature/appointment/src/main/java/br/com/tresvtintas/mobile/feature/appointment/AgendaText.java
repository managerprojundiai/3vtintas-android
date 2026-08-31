package br.com.tresvtintas.mobile.feature.appointment;

import android.content.Context;
import br.com.tresvtintas.mobile.core.appointment.AgendaFailureKind;
import java.util.Locale;
import java.util.Map;

final class AgendaText {
    private static final Map<String, Integer> STATUS = Map.of(
            "SCHEDULED", R.string.appointment_status_scheduled,
            "CONFIRMED", R.string.appointment_status_confirmed,
            "COMPLETED", R.string.appointment_status_completed,
            "CANCELLED", R.string.appointment_status_cancelled,
            "PENDING", R.string.agenda_status_pending,
            "SETTLED", R.string.agenda_status_settled,
            "SHIPPED", R.string.agenda_status_shipped,
            "IN_TRANSIT", R.string.agenda_status_in_transit,
            "DELIVERED", R.string.agenda_status_delivered,
            "FAILED", R.string.agenda_status_failed);

    private AgendaText() {
    }

    static String status(Context context, String value) {
        String normalized = value == null
                ? ""
                : value.trim().toUpperCase(Locale.ROOT);
        Integer resource = STATUS.get(normalized);
        return resource == null
                ? context.getString(R.string.agenda_status_unknown)
                : context.getString(resource);
    }

    static int failure(AgendaFailureKind value) {
        return AppointmentText.failure(
                br.com.tresvtintas.mobile.core.appointment
                        .AppointmentFailureKind.valueOf(value.name()));
    }
}
