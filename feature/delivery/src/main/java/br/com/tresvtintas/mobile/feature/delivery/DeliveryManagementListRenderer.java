package br.com.tresvtintas.mobile.feature.delivery;

import android.view.View;
import br.com.tresvtintas.mobile.core.delivery.DeliveryManagementListState;
import br.com.tresvtintas.mobile.feature.delivery.databinding.DeliveryManagementActivityListBinding;
import java.util.List;

final class DeliveryManagementListRenderer {
    private final DeliveryManagementActivityListBinding binding;
    private final DeliveryManagementSummaryAdapter adapter;

    DeliveryManagementListRenderer(
            DeliveryManagementActivityListBinding binding,
            DeliveryManagementSummaryAdapter adapter) {
        this.binding = binding;
        this.adapter = adapter;
    }

    void render(DeliveryManagementListState state) {
        boolean busy =
                state.phase() == DeliveryManagementListState.Phase.LOADING
                || state.phase()
                        == DeliveryManagementListState.Phase.REFRESHING
                || state.phase()
                        == DeliveryManagementListState.Phase.LOADING_MORE;
        binding.deliveryManagementProgress.setVisibility(
                busy ? View.VISIBLE : View.INVISIBLE);
        binding.deliveryManagementRefresh.setEnabled(!busy);
        binding.deliveryManagementNotice.setVisibility(View.GONE);
        binding.deliveryManagementRetry.setVisibility(View.GONE);
        if (state.phase() == DeliveryManagementListState.Phase.ERROR) {
            adapter.submitList(List.of());
            binding.deliveryManagementList.setVisibility(View.GONE);
            binding.deliveryManagementLoadMore.setVisibility(View.GONE);
            binding.deliveryManagementEmptyGroup.setVisibility(View.VISIBLE);
            binding.deliveryManagementEmptyTitle.setText(
                    R.string.delivery_management_title);
            binding.deliveryManagementEmptyMessage.setText(
                    DeliveryText.failure(state.failure().orElseThrow()));
            binding.deliveryManagementRetry.setVisibility(View.VISIBLE);
            support(state);
            return;
        }
        state.snapshot().ifPresentOrElse(
                value -> snapshot(state, value),
                () -> {
                    binding.deliveryManagementList.setVisibility(View.GONE);
                    binding.deliveryManagementLoadMore.setVisibility(View.GONE);
                    binding.deliveryManagementEmptyGroup.setVisibility(View.GONE);
                });
    }

    private void snapshot(
            DeliveryManagementListState state,
            DeliveryManagementListState.Snapshot snapshot) {
        adapter.submitList(snapshot.items());
        boolean empty = snapshot.items().isEmpty();
        binding.deliveryManagementList.setVisibility(
                empty ? View.GONE : View.VISIBLE);
        binding.deliveryManagementEmptyGroup.setVisibility(
                empty ? View.VISIBLE : View.GONE);
        binding.deliveryManagementLoadMore.setVisibility(
                !empty && snapshot.hasMore() ? View.VISIBLE : View.GONE);
        binding.deliveryManagementLoadMore.setEnabled(
                state.phase()
                        != DeliveryManagementListState.Phase.LOADING_MORE);
        if (state.failure().isPresent()) {
            binding.deliveryManagementNotice.setText(
                    R.string.delivery_warning_stale);
            binding.deliveryManagementNotice.setVisibility(View.VISIBLE);
            support(state);
        }
    }

    private void support(DeliveryManagementListState state) {
        state.requestId().ifPresent(value -> {
            binding.deliveryManagementNotice.setText(
                    binding.getRoot().getContext().getString(
                            R.string.delivery_request_id,
                            value));
            binding.deliveryManagementNotice.setVisibility(View.VISIBLE);
        });
    }
}
