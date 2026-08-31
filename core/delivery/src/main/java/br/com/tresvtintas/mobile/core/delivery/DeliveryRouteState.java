package br.com.tresvtintas.mobile.core.delivery;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

public record DeliveryRouteState(
        Phase phase,
        LocalDate serviceDate,
        Optional<DeliveryRoutePage> page,
        Optional<DeliveryFailureKind> failure,
        Optional<String> requestId) {
    public enum Phase {
        EMPTY,
        LOADING,
        REFRESHING,
        READY,
        ERROR,
        CLOSED
    }

    public DeliveryRouteState {
        Objects.requireNonNull(phase, "Route phase is required.");
        Objects.requireNonNull(serviceDate, "Route service date is required.");
        page = Objects.requireNonNull(page, "Route page is required.");
        failure = Objects.requireNonNull(failure, "Route failure is required.");
        requestId = Objects.requireNonNull(requestId, "Route request ID is required.");
    }

    public static DeliveryRouteState empty(LocalDate date) {
        return value(Phase.EMPTY, date, Optional.empty(), null);
    }

    public static DeliveryRouteState loading(LocalDate date) {
        return value(Phase.LOADING, date, Optional.empty(), null);
    }

    public static DeliveryRouteState refreshing(LocalDate date, DeliveryRoutePage page) {
        return value(Phase.REFRESHING, date, Optional.of(page), null);
    }

    public static DeliveryRouteState ready(LocalDate date, DeliveryRoutePage page) {
        return value(Phase.READY, date, Optional.of(page), null);
    }

    public static DeliveryRouteState error(LocalDate date, DeliveryException failure) {
        return new DeliveryRouteState(
                Phase.ERROR,
                date,
                Optional.empty(),
                Optional.of(failure.kind()),
                failure.requestId());
    }

    public static DeliveryRouteState closed(LocalDate date) {
        return value(Phase.CLOSED, date, Optional.empty(), null);
    }

    private static DeliveryRouteState value(
            Phase phase,
            LocalDate date,
            Optional<DeliveryRoutePage> page,
            DeliveryFailureKind failure) {
        return new DeliveryRouteState(
                phase,
                date,
                page,
                Optional.ofNullable(failure),
                Optional.empty());
    }
}
