package br.com.tresvtintas.mobile.feature.customer;

import android.view.View;
import br.com.tresvtintas.mobile.core.customer.CustomerListState;
import br.com.tresvtintas.mobile.core.customer.CustomerSnapshot;
import br.com.tresvtintas.mobile.feature.customer.databinding.CustomerActivityListBinding;

final class CustomerListRenderer {
    private final CustomerActivityListBinding binding;
    private final CustomerSummaryAdapter adapter;
    private boolean writeAllowed;

    CustomerListRenderer(
            CustomerActivityListBinding binding,
            CustomerSummaryAdapter adapter) {
        this.binding = binding;
        this.adapter = adapter;
    }

    void writeAllowed(boolean value) {
        writeAllowed = value;
        binding.customerAdd.setVisibility(value ? View.VISIBLE : View.GONE);
    }

    void render(CustomerListState state) {
        boolean busy = state.phase() == CustomerListState.Phase.LOADING
                || state.phase() == CustomerListState.Phase.REFRESHING
                || state.phase() == CustomerListState.Phase.LOADING_MORE;
        binding.customerProgress.setVisibility(
                busy ? View.VISIBLE : View.INVISIBLE);
        binding.customerRefresh.setEnabled(!busy);
        binding.customerAdd.setEnabled(!busy && writeAllowed);
        binding.customerNoticeCard.setVisibility(View.GONE);
        binding.customerRetry.setVisibility(View.GONE);

        if (state.phase() == CustomerListState.Phase.ERROR) {
            adapter.submitList(java.util.List.of());
            binding.customerList.setVisibility(View.GONE);
            binding.customerLoadMore.setVisibility(View.GONE);
            binding.customerEmptyGroup.setVisibility(View.VISIBLE);
            binding.customerEmptyTitle.setText(R.string.customer_list_title);
            binding.customerEmptyMessage.setText(CustomerFailureText.resource(
                    state.failure().orElseThrow()));
            binding.customerRetry.setVisibility(View.VISIBLE);
            supportCode(state);
            return;
        }

        state.snapshot().ifPresentOrElse(
                snapshot -> showSnapshot(state, snapshot),
                () -> {
                    binding.customerList.setVisibility(View.GONE);
                    binding.customerLoadMore.setVisibility(View.GONE);
                    binding.customerEmptyGroup.setVisibility(View.GONE);
                });
    }

    private void showSnapshot(
            CustomerListState state,
            CustomerSnapshot snapshot) {
        adapter.submitList(snapshot.items());
        boolean empty = snapshot.items().isEmpty();
        binding.customerList.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.customerEmptyGroup.setVisibility(
                empty ? View.VISIBLE : View.GONE);
        if (empty) {
            binding.customerEmptyTitle.setText(R.string.customer_empty_title);
            binding.customerEmptyMessage.setText(
                    R.string.customer_empty_message);
        }
        binding.customerLoadMore.setVisibility(
                !empty && snapshot.hasMore() ? View.VISIBLE : View.GONE);
        binding.customerLoadMore.setEnabled(
                state.phase() != CustomerListState.Phase.LOADING_MORE);
        if (state.failure().isPresent()) {
            binding.customerNotice.setText(R.string.customer_warning_stale);
            binding.customerNoticeCard.setVisibility(View.VISIBLE);
            supportCode(state);
        }
    }

    private void supportCode(CustomerListState state) {
        state.requestId().ifPresent(requestId -> {
            binding.customerNotice.setText(binding.getRoot().getContext()
                    .getString(
                            R.string.customer_support_code,
                            requestId));
            binding.customerNoticeCard.setVisibility(View.VISIBLE);
        });
    }
}
