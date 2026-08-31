package br.com.tresvtintas.mobile.core.delivery;

import java.util.Objects;
import java.util.Optional;

public record DeliveryDetailState(
        Phase phase,
        Optional<DeliveryDetail> delivery,
        Optional<DeliveryFailureKind> failure,
        Optional<String> requestId) {
    public enum Phase {
        EMPTY,
        LOADING,
        READY,
        ERROR,
        CLOSED
    }

    public DeliveryDetailState {
        Objects.requireNonNull(phase, "Delivery detail phase is required.");
        delivery = Objects.requireNonNull(delivery, "Delivery detail is required.");
        failure = Objects.requireNonNull(failure, "Delivery detail failure is required.");
        requestId = Objects.requireNonNull(requestId, "Delivery request ID is required.");
    }

    public static DeliveryDetailState empty() {
        return value(Phase.EMPTY, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static DeliveryDetailState loading() {
        return value(Phase.LOADING, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static DeliveryDetailState ready(DeliveryDetail delivery) {
        return value(Phase.READY, Optional.of(delivery), Optional.empty(), Optional.empty());
    }

    public static DeliveryDetailState error(DeliveryException failure) {
        return value(
                Phase.ERROR,
                Optional.empty(),
                Optional.of(failure.kind()),
                failure.requestId());
    }

    public static DeliveryDetailState closed() {
        return value(Phase.CLOSED, Optional.empty(), Optional.empty(), Optional.empty());
    }

    private static DeliveryDetailState value(
            Phase phase,
            Optional<DeliveryDetail> delivery,
            Optional<DeliveryFailureKind> failure,
            Optional<String> requestId) {
        return new DeliveryDetailState(phase, delivery, failure, requestId);
    }
}
