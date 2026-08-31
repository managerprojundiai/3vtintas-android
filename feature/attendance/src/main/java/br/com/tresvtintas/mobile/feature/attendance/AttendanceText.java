package br.com.tresvtintas.mobile.feature.attendance;

import android.content.Context;
import br.com.tresvtintas.mobile.core.attendance.AttendanceChannel;
import br.com.tresvtintas.mobile.core.attendance.AttendanceFailureKind;
import br.com.tresvtintas.mobile.core.attendance.AttendanceFolder;
import br.com.tresvtintas.mobile.core.attendance.AttendanceHandlingMode;
import br.com.tresvtintas.mobile.core.attendance.AttendancePriority;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

final class AttendanceText {
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm")
                    .withZone(ZoneId.of("America/Sao_Paulo"));

    private AttendanceText() {
        throw new AssertionError("No instances.");
    }

    static String channel(Context context, AttendanceChannel value) {
        return context.getString(
                value == AttendanceChannel.WHATSAPP
                        ? R.string.attendance_channel_whatsapp
                        : R.string.attendance_channel_site);
    }

    static String folder(Context context, AttendanceFolder value) {
        return context.getString(switch (value) {
            case INBOX -> R.string.attendance_folder_inbox;
            case MINE -> R.string.attendance_folder_mine;
            case UNASSIGNED -> R.string.attendance_folder_unassigned;
            case URGENT -> R.string.attendance_folder_urgent;
            case FOLLOW_UP -> R.string.attendance_folder_follow_up;
            case SUPPLIERS -> R.string.attendance_folder_suppliers;
            case VIP -> R.string.attendance_folder_vip;
            case RESOLVED -> R.string.attendance_folder_resolved;
        });
    }

    static String priority(
            Context context,
            AttendancePriority value) {
        return context.getString(switch (value) {
            case LOW -> R.string.attendance_priority_low;
            case NORMAL -> R.string.attendance_priority_normal;
            case HIGH -> R.string.attendance_priority_high;
            case URGENT -> R.string.attendance_priority_urgent;
        });
    }

    static String mode(
            Context context,
            AttendanceHandlingMode value) {
        return context.getString(
                value == AttendanceHandlingMode.AI
                        ? R.string.attendance_mode_ai
                        : R.string.attendance_mode_human);
    }

    static String activity(Instant value) {
        return DATE_TIME.format(value);
    }

    static int failure(AttendanceFailureKind kind) {
        return switch (kind) {
            case NETWORK, SERVICE_UNAVAILABLE ->
                    R.string.attendance_failure_network;
            case ACCESS_REVOKED, AUTH_REJECTED, FORBIDDEN ->
                    R.string.attendance_failure_forbidden;
            case UPDATE_REQUIRED ->
                    R.string.attendance_failure_update;
            case RATE_LIMITED ->
                    R.string.attendance_failure_rate;
            case CONFLICT ->
                    R.string.attendance_failure_channel_unavailable;
            case IDEMPOTENCY_IN_PROGRESS ->
                    R.string.attendance_failure_in_progress;
            case IDEMPOTENCY_KEY_REUSED ->
                    R.string.attendance_failure_generic;
            case NOT_FOUND ->
                    R.string.attendance_failure_not_found;
            default -> R.string.attendance_failure_generic;
        };
    }

    static boolean retryable(AttendanceFailureKind kind) {
        return kind == AttendanceFailureKind.NETWORK
                || kind == AttendanceFailureKind.RATE_LIMITED
                || kind == AttendanceFailureKind.CONFLICT
                || kind == AttendanceFailureKind.IDEMPOTENCY_IN_PROGRESS
                || kind == AttendanceFailureKind.SERVICE_UNAVAILABLE;
    }
}
