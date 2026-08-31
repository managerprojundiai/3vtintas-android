package br.com.tresvtintas.mobile.feature.finance;

import android.view.View;
import br.com.tresvtintas.mobile.core.finance.FinanceListState;
import br.com.tresvtintas.mobile.core.finance.FinanceOverview;
import br.com.tresvtintas.mobile.core.finance.FinanceSnapshot;
import br.com.tresvtintas.mobile.feature.finance.databinding.FinanceActivityListBinding;
import java.math.BigDecimal;
import java.util.List;

final class FinanceListRenderer {
    private final FinanceActivityListBinding binding;
    private final FinanceSummaryAdapter adapter;

    FinanceListRenderer(
            FinanceActivityListBinding binding,
            FinanceSummaryAdapter adapter) {
        this.binding = binding;
        this.adapter = adapter;
    }

    void render(FinanceListState state) {
        boolean busy = state.phase() == FinanceListState.Phase.LOADING
                || state.phase() == FinanceListState.Phase.REFRESHING
                || state.phase() == FinanceListState.Phase.LOADING_MORE;
        binding.financeProgress.setVisibility(
                busy ? View.VISIBLE : View.INVISIBLE);
        binding.financeRefresh.setEnabled(!busy);
        binding.financeCreate.setEnabled(!busy);
        binding.financeNoticeCard.setVisibility(View.GONE);
        binding.financeRetry.setVisibility(View.GONE);
        if (state.phase() == FinanceListState.Phase.ERROR) {
            adapter.submitList(List.of());
            binding.financeOverview.setVisibility(View.GONE);
            binding.financeList.setVisibility(View.GONE);
            binding.financeLoadMore.setVisibility(View.GONE);
            binding.financeEmptyGroup.setVisibility(View.VISIBLE);
            binding.financeEmptyTitle.setText(R.string.finance_empty_title);
            binding.financeEmptyMessage.setText(FinanceText.failure(
                    binding.getRoot().getContext(),
                    state.failure().orElseThrow()));
            binding.financeRetry.setVisibility(View.VISIBLE);
            support(state);
            return;
        }
        state.snapshot().ifPresentOrElse(
                value -> snapshot(state, value),
                () -> {
                    binding.financeOverview.setVisibility(View.GONE);
                    binding.financeList.setVisibility(View.GONE);
                    binding.financeLoadMore.setVisibility(View.GONE);
                    binding.financeEmptyGroup.setVisibility(View.GONE);
                });
    }

    private void snapshot(
            FinanceListState state,
            FinanceSnapshot snapshot) {
        adapter.submitList(snapshot.items());
        overview(snapshot.overview());
        binding.financeOverview.setVisibility(View.VISIBLE);
        boolean empty = snapshot.items().isEmpty();
        binding.financeList.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.financeEmptyGroup.setVisibility(
                empty ? View.VISIBLE : View.GONE);
        if (empty) {
            binding.financeEmptyTitle.setText(R.string.finance_empty_title);
            binding.financeEmptyMessage.setText(R.string.finance_empty_message);
        }
        binding.financeLoadMore.setVisibility(
                !empty && snapshot.hasMore() ? View.VISIBLE : View.GONE);
        binding.financeLoadMore.setEnabled(
                state.phase() != FinanceListState.Phase.LOADING_MORE);
        if (state.failure().isPresent()) {
            binding.financeNotice.setText(R.string.finance_warning_stale);
            binding.financeNoticeCard.setVisibility(View.VISIBLE);
            support(state);
        }
    }

    private void overview(FinanceOverview value) {
        binding.financeOverviewPending.setText(total(
                R.string.finance_overview_pending,
                value.pending()));
        binding.financeOverviewSettled.setText(total(
                R.string.finance_overview_settled,
                value.settled()));
        binding.financeOverviewCancelled.setText(total(
                R.string.finance_overview_cancelled,
                value.cancelled()));
        binding.financeOverviewOverdue.setText(total(
                R.string.finance_overview_overdue,
                value.overdue()));
    }

    private String total(
            int label,
            FinanceOverview.TypeTotals value) {
        int count = value.expense().count()
                + value.payable().count()
                + value.receivable().count();
        BigDecimal amount = value.expense().amount()
                .add(value.payable().amount())
                .add(value.receivable().amount());
        var context = binding.getRoot().getContext();
        return context.getString(label)
                + "\n"
                + context.getResources().getQuantityString(
                        R.plurals.finance_overview_value,
                        count,
                        FinanceText.money(amount),
                        count);
    }

    private void support(FinanceListState state) {
        state.requestId().ifPresent(value -> {
            binding.financeNotice.setText(
                    binding.getRoot().getContext().getString(
                            R.string.finance_support_code,
                            value));
            binding.financeNoticeCard.setVisibility(View.VISIBLE);
        });
    }
}
