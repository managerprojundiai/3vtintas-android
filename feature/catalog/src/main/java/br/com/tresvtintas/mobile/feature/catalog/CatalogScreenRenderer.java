package br.com.tresvtintas.mobile.feature.catalog;

import android.view.View;
import br.com.tresvtintas.mobile.core.catalog.CatalogFailureKind;
import br.com.tresvtintas.mobile.core.catalog.CatalogSnapshot;
import br.com.tresvtintas.mobile.feature.catalog.databinding.CatalogActivityBinding;

final class CatalogScreenRenderer {
    private final CatalogActivityBinding binding;
    private final CatalogProductAdapter adapter;

    CatalogScreenRenderer(
            CatalogActivityBinding binding,
            CatalogProductAdapter adapter) {
        this.binding = binding;
        this.adapter = adapter;
    }

    void render(CatalogUiState state) {
        binding.catalogInitialProgress.setVisibility(
                state.initialLoading() ? View.VISIBLE : View.GONE);
        binding.catalogRefreshProgress.setVisibility(
                state.refreshing() ? View.VISIBLE : View.INVISIBLE);
        binding.catalogList.setVisibility(state.showList() ? View.VISIBLE : View.GONE);
        binding.catalogEmpty.setVisibility(state.showEmpty() ? View.VISIBLE : View.GONE);
        binding.catalogEmpty.setText(state.priceSelectionRequired()
                ? R.string.catalog_price_selection_required
                : R.string.catalog_empty);
        binding.catalogErrorCard.setVisibility(state.showError() ? View.VISIBLE : View.GONE);
        binding.catalogRetry.setVisibility(state.retryAllowed() ? View.VISIBLE : View.GONE);
        state.snapshot().ifPresent(snapshot -> {
            adapter.submitList(snapshot.items());
            renderSnapshot(snapshot, state.failure());
        });
        if (state.showError()) {
            binding.catalogErrorMessage.setText(failureMessage(
                    state.failure().orElse(CatalogFailureKind.PROTOCOL)));
        }
        binding.catalogLoadMore.setVisibility(
                state.snapshot().map(CatalogSnapshot::hasMore).orElse(false)
                        ? View.VISIBLE
                        : View.GONE);
        binding.catalogLoadMore.setEnabled(!state.loadingMore());
        binding.catalogLoadMore.setText(state.loadingMore()
                ? R.string.catalog_loading_more
                : R.string.catalog_load_more);
        binding.catalogRequestId.setVisibility(
                state.requestId().isPresent() ? View.VISIBLE : View.GONE);
        state.requestId().ifPresent(requestId -> binding.catalogRequestId.setText(
                binding.getRoot().getContext().getString(
                        R.string.catalog_request_id,
                        requestId)));
    }

    private void renderSnapshot(
            CatalogSnapshot snapshot,
            java.util.Optional<CatalogFailureKind> warning) {
        if (warning.isPresent()) {
            binding.catalogStatus.setText(R.string.catalog_status_offline);
        } else if (snapshot.stale()) {
            binding.catalogStatus.setText(R.string.catalog_status_cache);
        } else {
            binding.catalogStatus.setText(
                    binding.getRoot().getContext().getResources().getQuantityString(
                            R.plurals.catalog_status_products,
                            snapshot.items().size(),
                            snapshot.items().size()));
        }
    }

    private static int failureMessage(CatalogFailureKind failure) {
        return switch (failure) {
            case ACCESS_REVOKED, FORBIDDEN -> R.string.catalog_error_forbidden;
            case AUTH_REJECTED -> R.string.catalog_error_auth;
            case INVALID_REQUEST, PROTOCOL -> R.string.catalog_error_protocol;
            case NETWORK -> R.string.catalog_error_network;
            case PRICE_NOT_AVAILABLE -> R.string.catalog_error_price_unavailable;
            case PRICE_POLICY_CHANGED -> R.string.catalog_error_price_changed;
            case PRICE_SELECTION_REQUIRED -> R.string.catalog_error_price_selection;
            case RATE_LIMITED -> R.string.catalog_error_rate_limited;
            case SERVICE_UNAVAILABLE -> R.string.catalog_error_service;
            case STORAGE -> R.string.catalog_error_storage;
            case UPDATE_REQUIRED -> R.string.catalog_error_update;
        };
    }
}
