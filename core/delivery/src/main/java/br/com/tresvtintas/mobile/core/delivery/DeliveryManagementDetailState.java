package br.com.tresvtintas.mobile.core.delivery;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record DeliveryManagementDetailState(
        Phase phase,
        Optional<DeliveryManagementDetail> detail,
        List<DeliveryManagementDriver> drivers,
        Optional<DeliveryFailureKind> failure,
        Optional<String> requestId) {
    public enum Phase {
        EMPTY,
        LOADING,
        READY,
        ERROR,
        CLOSED
    }

    public DeliveryManagementDetailState {
        Objects.requireNonNull(phase, "Management detail phase is required.");
        detail = Objects.requireNonNull(detail, "Management detail is required.");
        if (drivers == null
                || drivers.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Management drivers are invalid.");
        }
        drivers = List.copyOf(drivers);
        failure = Objects.requireNonNull(failure, "Management failure is required.");
        requestId = Objects.requireNonNull(requestId, "Management request ID is required.");
    }

    public static DeliveryManagementDetailState empty() {
        return value(Phase.EMPTY, Optional.empty(), List.of(), null);
    }

    public static DeliveryManagementDetailState loading() {
        return value(Phase.LOADING, Optional.empty(), List.of(), null);
    }

    public static DeliveryManagementDetailState ready(
            DeliveryManagementDetail detail,
            List<DeliveryManagementDriver> drivers) {
        return new DeliveryManagementDetailState(
                Phase.READY,
                Optional.of(detail),
                drivers,
                Optional.empty(),
                Optional.empty());
    }

    public static DeliveryManagementDetailState error(
            DeliveryException failure) {
        return value(Phase.ERROR, Optional.empty(), List.of(), failure);
    }

    public static DeliveryManagementDetailState closed() {
        return value(Phase.CLOSED, Optional.empty(), List.of(), null);
    }

    private static DeliveryManagementDetailState value(
            Phase phase,
            Optional<DeliveryManagementDetail> detail,
            List<DeliveryManagementDriver> drivers,
            DeliveryException failure) {
        return new DeliveryManagementDetailState(
                phase,
                detail,
                drivers,
                failure == null
                        ? Optional.empty()
                        : Optional.of(failure.kind()),
                failure == null
                        ? Optional.empty()
                        : failure.requestId());
    }
}
