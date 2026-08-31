package br.com.tresvtintas.mobile.feature.appointment;

import android.content.Context;
import br.com.tresvtintas.mobile.core.appointment.AppointmentFailureKind;
import br.com.tresvtintas.mobile.core.appointment.AppointmentKind;
import br.com.tresvtintas.mobile.core.appointment.AppointmentPerson;
import br.com.tresvtintas.mobile.core.appointment.AppointmentStatus;
import br.com.tresvtintas.mobile.core.model.AppRole;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

final class AppointmentText {
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    private AppointmentText() {
    }

    static String dateTime(Instant value) {
        return DATE_TIME.format(value.atZone(ZoneId.systemDefault()));
    }

    static String duration(Context context, int minutes) {
        return context.getString(
                R.string.appointment_duration_minutes,
                minutes);
    }

    static String kind(Context context, AppointmentKind value) {
        return context.getString(switch (value) {
            case GENERAL -> R.string.appointment_kind_general;
            case DELIVERY -> R.string.appointment_kind_delivery;
            case COLLECTION -> R.string.appointment_kind_collection;
        });
    }

    static String status(Context context, AppointmentStatus value) {
        return context.getString(switch (value) {
            case SCHEDULED -> R.string.appointment_status_scheduled;
            case CONFIRMED -> R.string.appointment_status_confirmed;
            case COMPLETED -> R.string.appointment_status_completed;
            case CANCELLED -> R.string.appointment_status_cancelled;
        });
    }

    static String person(Context context, AppointmentPerson value) {
        return value.name().orElseGet(() -> context.getString(
                R.string.appointment_person_fallback,
                value.id()));
    }

    static String role(Context context, AppRole value) {
        return context.getString(switch (value) {
            case MASTER_ADMIN -> R.string.appointment_role_master;
            case MANAGER -> R.string.appointment_role_manager;
            case SALESPERSON -> R.string.appointment_role_salesperson;
            case DELIVERY_DRIVER -> R.string.appointment_role_driver;
            case PAINTER -> R.string.appointment_role_painter;
            case CUSTOMER -> R.string.appointment_role_customer;
            case USER -> R.string.appointment_role_user;
        });
    }

    static int failure(AppointmentFailureKind value) {
        return switch (value) {
            case ACCESS_REVOKED, FORBIDDEN ->
                R.string.appointment_failure_access;
            case AUTH_REJECTED -> R.string.appointment_failure_auth;
            case CONFLICT -> R.string.appointment_failure_conflict;
            case IDEMPOTENCY_IN_PROGRESS ->
                R.string.appointment_failure_in_progress;
            case IDEMPOTENCY_KEY_REUSED, INVALID_REQUEST ->
                R.string.appointment_failure_invalid;
            case NETWORK -> R.string.appointment_failure_network;
            case NOT_FOUND -> R.string.appointment_failure_not_found;
            case PROTOCOL -> R.string.appointment_failure_protocol;
            case RATE_LIMITED -> R.string.appointment_failure_rate;
            case SERVICE_UNAVAILABLE ->
                R.string.appointment_failure_service;
            case UPDATE_REQUIRED ->
                R.string.appointment_failure_update;
        };
    }
}
