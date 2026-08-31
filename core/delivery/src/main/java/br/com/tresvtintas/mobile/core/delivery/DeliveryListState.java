package br.com.tresvtintas.mobile.core.delivery;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record DeliveryListState(
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

    public DeliveryListState {
        Objects.requireNonNull(phase, "Delivery list phase is required.");
        snapshot = Objects.requireNonNull(snapshot, "Delivery list snapshot is required.");
        failure = Objects.requireNonNull(failure, "Delivery list failure is required.");
        requestId = Objects.requireNonNull(requestId, "Delivery list request ID is required.");
    }

    public static DeliveryListState empty() {
        return state(Phase.EMPTY, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static DeliveryListState loading() {
        return state(Phase.LOADING, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static DeliveryListState refreshing(Snapshot value) {
        return state(Phase.REFRESHING, Optional.of(value), Optional.empty(), Optional.empty());
    }

    public static DeliveryListState loadingMore(Snapshot value) {
        return state(Phase.LOADING_MORE, Optional.of(value), Optional.empty(), Optional.empty());
    }

    public static DeliveryListState ready(
            Snapshot value,
            Optional<DeliveryFailureKind> warning,
            Optional<String> requestId) {
        return state(Phase.READY, Optional.of(value), warning, requestId);
    }

    public static DeliveryListState error(DeliveryException failure) {
        return state(
                Phase.ERROR,
                Optional.empty(),
                Optional.of(failure.kind()),
                failure.requestId());
    }

    public static DeliveryListState closed() {
        return state(Phase.CLOSED, Optional.empty(), Optional.empty(), Optional.empty());
    }

    private static DeliveryListState state(
            Phase phase,
            Optional<Snapshot> snapshot,
            Optional<DeliveryFailureKind> failure,
            Optional<String> requestId) {
        return new DeliveryListState(phase, snapshot, failure, requestId);
    }

    public record Snapshot(
            List<DeliverySummary> items,
            Optional<String> nextCursor) {
        public Snapshot {
            if (items == null
                    || items.stream().anyMatch(Objects::isNull)) {
                throw new IllegalArgumentException("Delivery snapshot is invalid.");
            }
            items = List.copyOf(items);
            nextCursor = Objects.requireNonNull(nextCursor, "Delivery cursor is required.");
        }

        public static Snapshot from(DeliveryPage page) {
            return new Snapshot(page.items(), page.nextCursor());
        }

        public Snapshot append(DeliveryPage page) {
            List<DeliverySummary> combined = new ArrayList<>(items);
            combined.addAll(page.items());
            return new Snapshot(combined, page.nextCursor());
        }

        public boolean hasMore() {
            return nextCursor.isPresent();
        }
    }
}
