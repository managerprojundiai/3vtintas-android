package br.com.tresvtintas.mobile.feature.delivery;

import android.content.Context;
import br.com.tresvtintas.mobile.core.delivery.DeliveryFailureKind;
import br.com.tresvtintas.mobile.core.delivery.DeliveryStatus;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

final class DeliveryText {
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm")
                    .withZone(ZoneId.of("America/Sao_Paulo"));

    private DeliveryText() {
        throw new AssertionError("No instances.");
    }

    static String status(Context context, DeliveryStatus status) {
        return context.getString(switch (status) {
            case PENDING -> R.string.delivery_status_pending;
            case SHIPPED -> R.string.delivery_status_shipped;
            case IN_TRANSIT -> R.string.delivery_status_in_transit;
            case DELIVERED -> R.string.delivery_status_delivered;
            case FAILED -> R.string.delivery_status_failed;
        });
    }

    static String schedule(Context context, Optional<Instant> value) {
        return value.map(DATE_TIME::format)
                .orElseGet(() -> context.getString(
                        R.string.delivery_unscheduled));
    }

    static String place(
            Context context,
            Optional<String> city,
            Optional<String> state) {
        if (city.isPresent() && state.isPresent()) {
            return city.orElseThrow() + "/" + state.orElseThrow();
        }
        return city.or(() -> state).orElseGet(() ->
                context.getString(R.string.delivery_not_informed));
    }

    static int failure(DeliveryFailureKind kind) {
        return switch (kind) {
            case NETWORK, SERVICE_UNAVAILABLE ->
                    R.string.delivery_failure_network;
            case ACCESS_REVOKED, AUTH_REJECTED, FORBIDDEN ->
                    R.string.delivery_failure_forbidden;
            case NOT_FOUND -> R.string.delivery_failure_not_found;
            case UPDATE_REQUIRED -> R.string.delivery_failure_update;
            case RATE_LIMITED, IDEMPOTENCY_IN_PROGRESS ->
                    R.string.delivery_failure_rate;
            default -> R.string.delivery_failure_generic;
        };
    }
}
