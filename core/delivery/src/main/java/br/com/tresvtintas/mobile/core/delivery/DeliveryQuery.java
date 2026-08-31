package br.com.tresvtintas.mobile.core.delivery;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

public record DeliveryQuery(
        Optional<String> search,
        OptionalLong organizationId,
        Optional<DeliveryStatus> status,
        DeliveryView view,
        Optional<Instant> scheduledFrom,
        Optional<Instant> scheduledToExclusive,
        int pageSize) {
    public DeliveryQuery {
        search = Objects.requireNonNull(search, "Delivery search is required.")
                .map(String::trim)
                .filter(value -> !value.isEmpty());
        organizationId = Objects.requireNonNull(
                organizationId,
                "Delivery organization is required.");
        status = Objects.requireNonNull(status, "Delivery status is required.");
        Objects.requireNonNull(view, "Delivery view is required.");
        scheduledFrom = Objects.requireNonNull(
                scheduledFrom,
                "Delivery schedule start is required.");
        scheduledToExclusive = Objects.requireNonNull(
                scheduledToExclusive,
                "Delivery schedule end is required.");
        boolean hasCompleteWindow = scheduledFrom.isPresent()
                && scheduledToExclusive.isPresent();
        if (search.map(String::length).orElse(0) > 80
                || (organizationId.isPresent() && organizationId.orElseThrow() < 1)
                || pageSize < 1
                || pageSize > 100
                || (status.isPresent() && view != DeliveryView.ALL)
                || (view == DeliveryView.CALENDAR) != hasCompleteWindow
                || (hasCompleteWindow
                        && !scheduledToExclusive.orElseThrow().isAfter(
                                scheduledFrom.orElseThrow()))) {
            throw new IllegalArgumentException("Delivery query is invalid.");
        }
    }

    public static DeliveryQuery initial() {
        return new DeliveryQuery(
                Optional.empty(),
                OptionalLong.empty(),
                Optional.empty(),
                DeliveryView.ACTIVE,
                Optional.empty(),
                Optional.empty(),
                30);
    }

    public DeliveryQuery withSearch(String value) {
        return new DeliveryQuery(
                Optional.ofNullable(value),
                organizationId,
                status,
                view,
                scheduledFrom,
                scheduledToExclusive,
                pageSize);
    }

    public DeliveryQuery withView(DeliveryView value) {
        return new DeliveryQuery(
                search,
                organizationId,
                Optional.empty(),
                value,
                Optional.empty(),
                Optional.empty(),
                pageSize);
    }

    public DeliveryQuery forOrganization(OptionalLong value) {
        return new DeliveryQuery(
                search,
                value == null ? OptionalLong.empty() : value,
                status,
                view,
                scheduledFrom,
                scheduledToExclusive,
                pageSize);
    }

    public DeliveryQuery forAgendaWindow(
            Instant from,
            Instant toExclusive) {
        return new DeliveryQuery(
                Optional.empty(),
                organizationId,
                Optional.empty(),
                DeliveryView.CALENDAR,
                Optional.ofNullable(from),
                Optional.ofNullable(toExclusive),
                100);
    }
}
