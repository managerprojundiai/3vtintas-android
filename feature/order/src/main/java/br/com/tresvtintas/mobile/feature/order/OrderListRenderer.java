package br.com.tresvtintas.mobile.feature.order;

import android.view.View;
import br.com.tresvtintas.mobile.core.order.OrderListState;
import br.com.tresvtintas.mobile.core.order.OrderSnapshot;
import br.com.tresvtintas.mobile.feature.order.databinding.OrderActivityListBinding;
import java.util.List;

final class OrderListRenderer {
    private final OrderActivityListBinding binding;
    private final OrderSummaryAdapter adapter;

    OrderListRenderer(OrderActivityListBinding binding, OrderSummaryAdapter adapter) {
        this.binding = binding;
        this.adapter = adapter;
    }

    void render(OrderListState state) {
        boolean busy = state.phase() == OrderListState.Phase.LOADING
                || state.phase() == OrderListState.Phase.REFRESHING
                || state.phase() == OrderListState.Phase.LOADING_MORE;
        binding.orderProgress.setVisibility(busy ? View.VISIBLE : View.INVISIBLE);
        binding.orderRefresh.setEnabled(!busy);
        binding.orderNoticeCard.setVisibility(View.GONE);
        binding.orderRetry.setVisibility(View.GONE);
        if (state.phase() == OrderListState.Phase.ERROR) {
            adapter.submitList(List.of());
            binding.orderList.setVisibility(View.GONE);
            binding.orderLoadMore.setVisibility(View.GONE);
            binding.orderEmptyGroup.setVisibility(View.VISIBLE);
            binding.orderEmptyTitle.setText(R.string.order_list_title);
            binding.orderEmptyMessage.setText(OrderText.failure(state.failure().orElseThrow()));
            binding.orderRetry.setVisibility(View.VISIBLE);
            support(state);
            return;
        }
        state.snapshot().ifPresentOrElse(value -> snapshot(state, value), () -> {
            binding.orderList.setVisibility(View.GONE);
            binding.orderLoadMore.setVisibility(View.GONE);
            binding.orderEmptyGroup.setVisibility(View.GONE);
        });
    }
    private void snapshot(OrderListState state, OrderSnapshot snapshot) {
        adapter.submitList(snapshot.items());
        boolean empty = snapshot.items().isEmpty();
        binding.orderList.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.orderEmptyGroup.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) {
            binding.orderEmptyTitle.setText(R.string.order_empty_title);
            binding.orderEmptyMessage.setText(R.string.order_empty_message);
        }
        binding.orderLoadMore.setVisibility(!empty && snapshot.hasMore() ? View.VISIBLE : View.GONE);
        binding.orderLoadMore.setEnabled(state.phase() != OrderListState.Phase.LOADING_MORE);
        if (state.failure().isPresent()) {
            binding.orderNotice.setText(R.string.order_warning_stale);
            binding.orderNoticeCard.setVisibility(View.VISIBLE);
            support(state);
        }
    }
    private void support(OrderListState state) {
        state.requestId().ifPresent(value -> {
            binding.orderNotice.setText(binding.getRoot().getContext().getString(R.string.order_support_code, value));
            binding.orderNoticeCard.setVisibility(View.VISIBLE);
        });
    }
}
