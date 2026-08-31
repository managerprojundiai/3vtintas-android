package br.com.tresvtintas.mobile.feature.delivery;

import android.content.Context;
import br.com.tresvtintas.mobile.core.delivery.DeliveryRouteStatus;
import br.com.tresvtintas.mobile.core.delivery.DeliveryRouteStopStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

final class DeliveryRouteText {
    private static final int METERS_PER_KILOMETER = 1_000;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final Locale PORTUGUESE_BRAZIL = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern(
            "EEE, dd 'de' MMM 'de' yyyy",
            PORTUGUESE_BRAZIL);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm")
            .withZone(BUSINESS_ZONE);

    private DeliveryRouteText() {
        throw new AssertionError("No instances.");
    }

    static String date(LocalDate value) {
        String formatted = DATE.format(value);
        return Character.toUpperCase(formatted.charAt(0)) + formatted.substring(1);
    }

    static int status(DeliveryRouteStatus value) {
        return switch (value) {
            case DRAFT -> R.string.delivery_route_status_draft;
            case CONFIRMED -> R.string.delivery_route_status_confirmed;
            case IN_PROGRESS -> R.string.delivery_route_status_in_progress;
            case COMPLETED -> R.string.delivery_route_status_completed;
            case CANCELLED -> R.string.delivery_route_status_cancelled;
        };
    }

    static int stopStatus(DeliveryRouteStopStatus value) {
        return switch (value) {
            case PLANNED -> R.string.delivery_route_stop_planned;
            case EN_ROUTE -> R.string.delivery_route_stop_en_route;
            case ARRIVED -> R.string.delivery_route_stop_arrived;
            case COMPLETED -> R.string.delivery_route_stop_completed;
            case SKIPPED -> R.string.delivery_route_stop_skipped;
        };
    }

    static String arrival(Context context, Optional<Instant> value) {
        return value.map(TIME::format).orElseGet(() ->
                context.getString(R.string.delivery_route_eta_unknown));
    }

    static String distance(Context context, int meters) {
        if (meters < METERS_PER_KILOMETER) {
            return context.getString(R.string.delivery_route_distance_meters, meters);
        }
        return context.getString(
                R.string.delivery_route_distance_kilometers,
                meters / (double) METERS_PER_KILOMETER);
    }

    static String duration(Context context, int seconds) {
        int minutes = Math.max(1, (seconds + 59) / 60);
        return context.getString(R.string.delivery_route_duration_minutes, minutes);
    }
}
