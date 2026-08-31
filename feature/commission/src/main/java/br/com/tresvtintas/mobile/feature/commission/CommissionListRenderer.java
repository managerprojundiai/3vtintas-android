package br.com.tresvtintas.mobile.feature.commission;

import android.view.View;
import br.com.tresvtintas.mobile.core.commission.CommissionListState;
import br.com.tresvtintas.mobile.core.commission.CommissionOverview;
import br.com.tresvtintas.mobile.core.commission.CommissionSnapshot;
import br.com.tresvtintas.mobile.feature.commission.databinding.CommissionActivityListBinding;
import java.util.List;

final class CommissionListRenderer {
    private final CommissionActivityListBinding binding;
    private final CommissionSummaryAdapter adapter;

    CommissionListRenderer(
            CommissionActivityListBinding binding,
            CommissionSummaryAdapter adapter) {
        this.binding = binding;
        this.adapter = adapter;
    }

    void render(CommissionListState state) {
        boolean busy = state.phase() == CommissionListState.Phase.LOADING
                || state.phase() == CommissionListState.Phase.REFRESHING
                || state.phase() == CommissionListState.Phase.LOADING_MORE;
        binding.commissionProgress.setVisibility(
                busy ? View.VISIBLE : View.INVISIBLE);
        binding.commissionRefresh.setEnabled(!busy);
        binding.commissionNoticeCard.setVisibility(View.GONE);
        binding.commissionRetry.setVisibility(View.GONE);
        if (state.phase() == CommissionListState.Phase.ERROR) {
            adapter.submitList(List.of());
            binding.commissionOverview.setVisibility(View.GONE);
            binding.commissionList.setVisibility(View.GONE);
            binding.commissionLoadMore.setVisibility(View.GONE);
            binding.commissionEmptyGroup.setVisibility(View.VISIBLE);
            binding.commissionEmptyTitle.setText(
                    R.string.commission_empty_title);
            binding.commissionEmptyMessage.setText(CommissionText.failure(
                    state.failure().orElseThrow()));
            binding.commissionRetry.setVisibility(View.VISIBLE);
            support(state);
            return;
        }
        state.snapshot().ifPresentOrElse(
                value -> snapshot(state, value),
                () -> {
                    binding.commissionOverview.setVisibility(View.GONE);
                    binding.commissionList.setVisibility(View.GONE);
                    binding.commissionLoadMore.setVisibility(View.GONE);
                    binding.commissionEmptyGroup.setVisibility(View.GONE);
                });
    }

    private void snapshot(
            CommissionListState state,
            CommissionSnapshot snapshot) {
        adapter.submitList(snapshot.items());
        overview(snapshot.overview());
        binding.commissionOverview.setVisibility(View.VISIBLE);
        boolean empty = snapshot.items().isEmpty();
        binding.commissionList.setVisibility(
                empty ? View.GONE : View.VISIBLE);
        binding.commissionEmptyGroup.setVisibility(
                empty ? View.VISIBLE : View.GONE);
        if (empty) {
            binding.commissionEmptyTitle.setText(
                    R.string.commission_empty_title);
            binding.commissionEmptyMessage.setText(
                    R.string.commission_empty_message);
        }
        binding.commissionLoadMore.setVisibility(
                !empty && snapshot.hasMore() ? View.VISIBLE : View.GONE);
        binding.commissionLoadMore.setEnabled(
                state.phase() != CommissionListState.Phase.LOADING_MORE);
        if (state.failure().isPresent()) {
            binding.commissionNotice.setText(
                    R.string.commission_warning_stale);
            binding.commissionNoticeCard.setVisibility(View.VISIBLE);
            support(state);
        }
    }

    private void overview(CommissionOverview value) {
        binding.commissionOverviewPending.setText(total(
                R.string.commission_overview_pending,
                value.pending()));
        binding.commissionOverviewApproved.setText(total(
                R.string.commission_overview_approved,
                value.approved()));
        binding.commissionOverviewPaid.setText(total(
                R.string.commission_overview_paid,
                value.paid()));
        binding.commissionOverviewCancelled.setText(total(
                R.string.commission_overview_cancelled,
                value.cancelled()));
    }

    private String total(
            int label,
            CommissionOverview.Totals value) {
        var context = binding.getRoot().getContext();
        return context.getString(label)
                + "\n"
                + context.getString(
                        R.string.commission_overview_value,
                        CommissionText.money(value.amount()),
                        value.count());
    }

    private void support(CommissionListState state) {
        state.requestId().ifPresent(value -> {
            binding.commissionNotice.setText(
                    binding.getRoot().getContext().getString(
                            R.string.commission_support_code,
                            value));
            binding.commissionNoticeCard.setVisibility(View.VISIBLE);
        });
    }
}
