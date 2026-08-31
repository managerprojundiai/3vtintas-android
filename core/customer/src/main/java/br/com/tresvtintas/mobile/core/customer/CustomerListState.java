package br.com.tresvtintas.mobile.core.customer;

import java.util.Optional;

public record CustomerListState(
        Phase phase,
        Optional<CustomerSnapshot> snapshot,
        Optional<CustomerFailureKind> failure,
        Optional<String> requestId) {
    public enum Phase {
        EMPTY,
        LOADING,
        READY,
        REFRESHING,
        LOADING_MORE,
        ERROR,
        CLOSED
    }

    public CustomerListState {
        if (phase == null) {
            throw new IllegalArgumentException("Customer list phase is required.");
        }
        snapshot = snapshot == null ? Optional.empty() : snapshot;
        failure = failure == null ? Optional.empty() : failure;
        requestId = requestId == null ? Optional.empty() : requestId;
        if ((phase == Phase.READY
                || phase == Phase.REFRESHING
                || phase == Phase.LOADING_MORE)
                && snapshot.isEmpty()) {
            throw new IllegalArgumentException(
                    "Customer list phase requires a snapshot.");
        }
    }

    public static CustomerListState empty() {
        return phase(Phase.EMPTY);
    }

    public static CustomerListState loading() {
        return phase(Phase.LOADING);
    }

    public static CustomerListState refreshing(CustomerSnapshot snapshot) {
        return withSnapshot(Phase.REFRESHING, snapshot);
    }

    public static CustomerListState loadingMore(CustomerSnapshot snapshot) {
        return withSnapshot(Phase.LOADING_MORE, snapshot);
    }

    public static CustomerListState ready(
            CustomerSnapshot snapshot,
            Optional<CustomerFailureKind> warning,
            Optional<String> requestId) {
        return new CustomerListState(
                Phase.READY,
                Optional.of(snapshot),
                warning,
                requestId);
    }

    public static CustomerListState error(CustomerException exception) {
        return new CustomerListState(
                Phase.ERROR,
                Optional.empty(),
                Optional.of(exception.kind()),
                exception.requestId());
    }

    public static CustomerListState closed() {
        return phase(Phase.CLOSED);
    }

    private static CustomerListState phase(Phase phase) {
        return new CustomerListState(
                phase,
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    private static CustomerListState withSnapshot(
            Phase phase,
            CustomerSnapshot snapshot) {
        return new CustomerListState(
                phase,
                Optional.of(snapshot),
                Optional.empty(),
                Optional.empty());
    }
}
