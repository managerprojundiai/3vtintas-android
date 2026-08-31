package br.com.tresvtintas.mobile.core.customer;

import java.util.Optional;

public record CustomerDetailState(
        Phase phase,
        Optional<CustomerDetail> customer,
        Optional<CustomerFailureKind> failure,
        Optional<String> requestId) {
    public enum Phase {
        EMPTY,
        LOADING,
        READY,
        ERROR,
        CLOSED
    }

    public CustomerDetailState {
        if (phase == null) {
            throw new IllegalArgumentException("Customer detail phase is required.");
        }
        customer = customer == null ? Optional.empty() : customer;
        failure = failure == null ? Optional.empty() : failure;
        requestId = requestId == null ? Optional.empty() : requestId;
        if (phase == Phase.READY && customer.isEmpty()) {
            throw new IllegalArgumentException(
                    "Ready customer detail requires a customer.");
        }
    }

    public static CustomerDetailState empty() {
        return phase(Phase.EMPTY);
    }

    public static CustomerDetailState loading() {
        return phase(Phase.LOADING);
    }

    public static CustomerDetailState ready(CustomerDetail customer) {
        return new CustomerDetailState(
                Phase.READY,
                Optional.of(customer),
                Optional.empty(),
                Optional.empty());
    }

    public static CustomerDetailState error(CustomerException exception) {
        return new CustomerDetailState(
                Phase.ERROR,
                Optional.empty(),
                Optional.of(exception.kind()),
                exception.requestId());
    }

    public static CustomerDetailState closed() {
        return phase(Phase.CLOSED);
    }

    private static CustomerDetailState phase(Phase phase) {
        return new CustomerDetailState(
                phase,
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }
}
