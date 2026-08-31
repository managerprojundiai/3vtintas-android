package br.com.tresvtintas.mobile.core.delivery;

import java.util.Objects;
import java.util.Optional;

public record DeliveryManagementQuery(
        long organizationId,
        Optional<String> search,
        DeliveryManagementView view,
        int pageSize) {
    private static final int DEFAULT_PAGE_SIZE = 30;
    private static final int MAXIMUM_PAGE_SIZE = 100;
    private static final int MAXIMUM_SEARCH_LENGTH = 80;
    private static final long MINIMUM_ORGANIZATION_ID = 1;
    private static final int MINIMUM_PAGE_SIZE = 1;

    public DeliveryManagementQuery {
        if (organizationId < MINIMUM_ORGANIZATION_ID
                || pageSize < MINIMUM_PAGE_SIZE
                || pageSize > MAXIMUM_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "Delivery management query is invalid.");
        }
        search = Objects.requireNonNull(search, "Search is required.")
                .map(String::trim)
                .filter(value -> !value.isEmpty());
        if (search.map(String::length).orElse(0)
                > MAXIMUM_SEARCH_LENGTH) {
            throw new IllegalArgumentException(
                    "Delivery management search is too long.");
        }
        Objects.requireNonNull(view, "Delivery management view is required.");
    }

    public static DeliveryManagementQuery initial(long organizationId) {
        return new DeliveryManagementQuery(
                organizationId,
                Optional.empty(),
                DeliveryManagementView.TODAY,
                DEFAULT_PAGE_SIZE);
    }

    public DeliveryManagementQuery withSearch(String value) {
        return new DeliveryManagementQuery(
                organizationId,
                Optional.ofNullable(value),
                view,
                pageSize);
    }

    public DeliveryManagementQuery withView(DeliveryManagementView value) {
        return new DeliveryManagementQuery(
                organizationId,
                search,
                value,
                pageSize);
    }
}
