package br.com.tresvtintas.mobile.feature.catalog;

import br.com.tresvtintas.mobile.core.catalog.CatalogFailureKind;
import br.com.tresvtintas.mobile.core.catalog.CatalogSnapshot;
import br.com.tresvtintas.mobile.core.catalog.CatalogState;
import java.util.Optional;

record CatalogUiState(
        boolean initialLoading,
        boolean refreshing,
        boolean loadingMore,
        boolean showList,
        boolean showEmpty,
        boolean showError,
        boolean priceSelectionRequired,
        boolean retryAllowed,
        Optional<CatalogSnapshot> snapshot,
        Optional<CatalogFailureKind> failure,
        Optional<String> requestId) {

    static CatalogUiState from(CatalogState state) {
        Optional<CatalogSnapshot> snapshot = state.snapshot();
        boolean hasItems = snapshot.map(item -> !item.items().isEmpty()).orElse(false);
        boolean ready = state.phase() == CatalogState.Phase.READY
                || state.phase() == CatalogState.Phase.REFRESHING
                || state.phase() == CatalogState.Phase.LOADING_MORE;
        boolean terminalFailure = state.phase() == CatalogState.Phase.ERROR
                || state.phase() == CatalogState.Phase.CLOSED;
        boolean priceSelectionRequired =
                state.phase() == CatalogState.Phase.PRICE_SELECTION_REQUIRED;
        return new CatalogUiState(
                state.phase() == CatalogState.Phase.EMPTY
                        || state.phase() == CatalogState.Phase.LOADING,
                state.phase() == CatalogState.Phase.REFRESHING,
                state.phase() == CatalogState.Phase.LOADING_MORE,
                ready && hasItems,
                (ready && !hasItems) || priceSelectionRequired,
                terminalFailure,
                priceSelectionRequired,
                retryAllowed(state),
                snapshot,
                state.failure(),
                state.requestId());
    }

    private static boolean retryAllowed(CatalogState state) {
        CatalogFailureKind failure = state.failure().orElse(null);
        return state.phase() != CatalogState.Phase.CLOSED
                && failure != CatalogFailureKind.ACCESS_REVOKED
                && failure != CatalogFailureKind.AUTH_REJECTED
                && failure != CatalogFailureKind.FORBIDDEN
                && failure != CatalogFailureKind.UPDATE_REQUIRED;
    }
}
