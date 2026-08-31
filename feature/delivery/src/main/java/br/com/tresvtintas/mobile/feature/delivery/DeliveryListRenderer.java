package br.com.tresvtintas.mobile.feature.delivery;

import android.view.View;
import br.com.tresvtintas.mobile.core.delivery.DeliveryListState;
import br.com.tresvtintas.mobile.feature.delivery.databinding.DeliveryActivityListBinding;
import java.util.List;

final class DeliveryListRenderer {
    private final DeliveryActivityListBinding binding;
    private final DeliverySummaryAdapter adapter;

    DeliveryListRenderer(
            DeliveryActivityListBinding binding,
            DeliverySummaryAdapter adapter) {
        this.binding = binding;
        this.adapter = adapter;
    }

    void render(DeliveryListState state) {
        boolean busy = state.phase() == DeliveryListState.Phase.LOADING
                || state.phase() == DeliveryListState.Phase.REFRESHING
                || state.phase() == DeliveryListState.Phase.LOADING_MORE;
        binding.deliveryProgress.setVisibility(
                busy ? View.VISIBLE : View.INVISIBLE);
        binding.deliveryRefresh.setEnabled(!busy);
        binding.deliveryNotice.setVisibility(View.GONE);
        binding.deliveryRetry.setVisibility(View.GONE);
        if (state.phase() == DeliveryListState.Phase.ERROR) {
            adapter.submitList(List.of());
            binding.deliveryList.setVisibility(View.GONE);
            binding.deliveryLoadMore.setVisibility(View.GONE);
            binding.deliveryEmptyGroup.setVisibility(View.VISIBLE);
            binding.deliveryEmptyTitle.setText(
                    R.string.delivery_list_title);
            binding.deliveryEmptyMessage.setText(DeliveryText.failure(
                    state.failure().orElseThrow()));
            binding.deliveryRetry.setVisibility(View.VISIBLE);
            support(state);
            return;
        }
        state.snapshot().ifPresentOrElse(
                value -> snapshot(state, value),
                () -> {
                    binding.deliveryList.setVisibility(View.GONE);
                    binding.deliveryLoadMore.setVisibility(View.GONE);
                    binding.deliveryEmptyGroup.setVisibility(View.GONE);
                });
    }

    private void snapshot(
            DeliveryListState state,
            DeliveryListState.Snapshot snapshot) {
        adapter.submitList(snapshot.items());
        boolean empty = snapshot.items().isEmpty();
        binding.deliveryList.setVisibility(
                empty ? View.GONE : View.VISIBLE);
        binding.deliveryEmptyGroup.setVisibility(
                empty ? View.VISIBLE : View.GONE);
        if (empty) {
            binding.deliveryEmptyTitle.setText(
                    R.string.delivery_empty_title);
            binding.deliveryEmptyMessage.setText(
                    R.string.delivery_empty_message);
        }
        binding.deliveryLoadMore.setVisibility(
                !empty && snapshot.hasMore()
                        ? View.VISIBLE
                        : View.GONE);
        binding.deliveryLoadMore.setEnabled(
                state.phase() != DeliveryListState.Phase.LOADING_MORE);
        if (state.failure().isPresent()) {
            binding.deliveryNotice.setText(
                    R.string.delivery_warning_stale);
            binding.deliveryNotice.setVisibility(View.VISIBLE);
            support(state);
        }
    }

    private void support(DeliveryListState state) {
        state.requestId().ifPresent(value -> {
            binding.deliveryNotice.setText(binding.getRoot()
                    .getContext()
                    .getString(
                            R.string.delivery_request_id,
                            value));
            binding.deliveryNotice.setVisibility(View.VISIBLE);
        });
    }
}
