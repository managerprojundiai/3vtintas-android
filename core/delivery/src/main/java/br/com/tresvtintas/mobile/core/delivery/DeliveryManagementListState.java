package br.com.tresvtintas.mobile.core.delivery;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record DeliveryManagementListState(
        Phase phase,
        Optional<Snapshot> snapshot,
        Optional<DeliveryFailureKind> failure,
        Optional<String> requestId) {
    public enum Phase {
        EMPTY,
        LOADING,
        REFRESHING,
        LOADING_MORE,
        READY,
        ERROR,
        CLOSED
    }

    public DeliveryManagementListState {
        Objects.requireNonNull(phase, "Management list phase is required.");
        snapshot = Objects.requireNonNull(snapshot, "Management snapshot is required.");
        failure = Objects.requireNonNull(failure, "Management failure is required.");
        requestId = Objects.requireNonNull(requestId, "Management request ID is required.");
    }

    public static DeliveryManagementListState empty() {
        return value(Phase.EMPTY, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static DeliveryManagementListState loading() {
        return value(Phase.LOADING, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static DeliveryManagementListState refreshing(Snapshot snapshot) {
        return value(Phase.REFRESHING, Optional.of(snapshot), Optional.empty(), Optional.empty());
    }

    public static DeliveryManagementListState loadingMore(Snapshot snapshot) {
        return value(Phase.LOADING_MORE, Optional.of(snapshot), Optional.empty(), Optional.empty());
    }

    public static DeliveryManagementListState ready(Snapshot snapshot) {
        return value(Phase.READY, Optional.of(snapshot), Optional.empty(), Optional.empty());
    }

    public static DeliveryManagementListState warning(
            Snapshot snapshot,
            DeliveryException failure) {
        return value(
                Phase.READY,
                Optional.of(snapshot),
                Optional.of(failure.kind()),
                failure.requestId());
    }

    public static DeliveryManagementListState error(DeliveryException failure) {
        return value(
                Phase.ERROR,
                Optional.empty(),
                Optional.of(failure.kind()),
                failure.requestId());
    }

    public static DeliveryManagementListState closed() {
        return value(Phase.CLOSED, Optional.empty(), Optional.empty(), Optional.empty());
    }

    private static DeliveryManagementListState value(
            Phase phase,
            Optional<Snapshot> snapshot,
            Optional<DeliveryFailureKind> failure,
            Optional<String> requestId) {
        return new DeliveryManagementListState(
                phase,
                snapshot,
                failure,
                requestId);
    }

    public record Snapshot(
            List<DeliveryManagementOrganization> organizations,
            DeliveryManagementOrganization selectedOrganization,
            DeliveryManagementQuery query,
            List<DeliveryManagementSummary> items,
            Optional<String> nextCursor) {
        public Snapshot {
            if (organizations == null
                    || organizations.isEmpty()
                    || organizations.stream().anyMatch(Objects::isNull)
                    || !organizations.contains(selectedOrganization)
                    || items == null
                    || items.stream().anyMatch(Objects::isNull)) {
                throw new IllegalArgumentException(
                        "Delivery management snapshot is invalid.");
            }
            organizations = List.copyOf(organizations);
            Objects.requireNonNull(query, "Management query is required.");
            if (query.organizationId() != selectedOrganization.id()) {
                throw new IllegalArgumentException(
                        "Management organization and query differ.");
            }
            items = List.copyOf(items);
            nextCursor = Objects.requireNonNull(
                    nextCursor,
                    "Management cursor is required.");
        }

        public Snapshot append(DeliveryManagementPage page) {
            List<DeliveryManagementSummary> combined =
                    new ArrayList<>(items);
            combined.addAll(page.items());
            return new Snapshot(
                    organizations,
                    selectedOrganization,
                    query,
                    combined,
                    page.nextCursor());
        }

        public boolean hasMore() {
            return nextCursor.isPresent();
        }
    }
}
