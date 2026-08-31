package br.com.tresvtintas.mobile.core.catalog;

import java.util.Optional;

public record CatalogState(
        Phase phase,
        Optional<CatalogSnapshot> snapshot,
        Optional<CatalogFailureKind> failure,
        Optional<String> requestId) {
    public enum Phase {
        EMPTY,
        LOADING,
        READY,
        REFRESHING,
        LOADING_MORE,
        PRICE_SELECTION_REQUIRED,
        ERROR,
        CLOSED
    }

    public CatalogState {
        if (phase == null) {
            throw new IllegalArgumentException("Catalog phase is required.");
        }
        snapshot = snapshot == null ? Optional.empty() : snapshot;
        failure = failure == null ? Optional.empty() : failure;
        requestId = requestId == null ? Optional.empty() : requestId;
        if ((phase == Phase.READY
                || phase == Phase.REFRESHING
                || phase == Phase.LOADING_MORE)
                && snapshot.isEmpty()) {
            throw new IllegalArgumentException("Catalog phase requires a snapshot.");
        }
    }

    public static CatalogState empty() {
        return phase(Phase.EMPTY);
    }

    public static CatalogState loading() {
        return phase(Phase.LOADING);
    }

    public static CatalogState refreshing(CatalogSnapshot snapshot) {
        return withSnapshot(Phase.REFRESHING, snapshot, Optional.empty());
    }

    public static CatalogState loadingMore(CatalogSnapshot snapshot) {
        return withSnapshot(Phase.LOADING_MORE, snapshot, Optional.empty());
    }

    public static CatalogState priceSelectionRequired() {
        return phase(Phase.PRICE_SELECTION_REQUIRED);
    }

    public static CatalogState ready(
            CatalogSnapshot snapshot,
            Optional<CatalogFailureKind> warning,
            Optional<String> requestId) {
        return new CatalogState(
                Phase.READY,
                Optional.of(snapshot),
                warning,
                requestId);
    }

    public static CatalogState error(CatalogException exception) {
        return new CatalogState(
                Phase.ERROR,
                Optional.empty(),
                Optional.of(exception.kind()),
                exception.requestId());
    }

    public static CatalogState closed() {
        return phase(Phase.CLOSED);
    }

    private static CatalogState phase(Phase phase) {
        return new CatalogState(
                phase,
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    private static CatalogState withSnapshot(
            Phase phase,
            CatalogSnapshot snapshot,
            Optional<CatalogFailureKind> failure) {
        return new CatalogState(
                phase,
                Optional.of(snapshot),
                failure,
                Optional.empty());
    }
}
