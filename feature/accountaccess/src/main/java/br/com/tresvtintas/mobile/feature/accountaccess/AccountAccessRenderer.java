package br.com.tresvtintas.mobile.feature.accountaccess;

import android.view.View;
import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessSnapshot;
import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessState;
import br.com.tresvtintas.mobile.core.accountaccess.AccountAccessView;
import br.com.tresvtintas.mobile.feature.accountaccess.databinding.AccountAccessActivityBinding;
import java.util.ArrayList;
import java.util.List;

final class AccountAccessRenderer {
    private final AccountAccessActivityBinding binding;
    private final AccountAccessAdapter adapter;

    AccountAccessRenderer(
            AccountAccessActivityBinding binding,
            AccountAccessAdapter adapter) {
        this.binding = binding;
        this.adapter = adapter;
    }

    void render(AccountAccessState state) {
        boolean busy = busy(state);
        boolean revoking =
                state.phase() == AccountAccessState.Phase.REVOKING;
        binding.accountAccessProgress.setVisibility(
                busy ? View.VISIBLE : View.INVISIBLE);
        binding.accountAccessRefresh.setEnabled(!busy);
        binding.accountAccessDevices.setEnabled(!revoking);
        binding.accountAccessSessions.setEnabled(!revoking);
        binding.accountAccessNoticeCard.setVisibility(View.GONE);
        binding.accountAccessRetry.setVisibility(View.GONE);
        select(state.view());
        if (state.phase() == AccountAccessState.Phase.ERROR) {
            error(state);
            return;
        }
        state.snapshot().ifPresentOrElse(
                value -> snapshot(state, value),
                () -> emptyState());
    }

    private void error(AccountAccessState state) {
        adapter.submitList(List.of());
        binding.accountAccessList.setVisibility(View.GONE);
        binding.accountAccessLoadMore.setVisibility(View.GONE);
        binding.accountAccessEmptyGroup.setVisibility(View.VISIBLE);
        binding.accountAccessEmptyTitle.setText(
                R.string.account_access_error_title);
        var failure = state.failure().orElseThrow();
        binding.accountAccessEmptyMessage.setText(
                AccountAccessText.failure(failure));
        binding.accountAccessRetry.setVisibility(
                AccountAccessText.retryable(failure)
                        ? View.VISIBLE
                        : View.GONE);
        support(state);
    }

    private void snapshot(
            AccountAccessState state,
            AccountAccessSnapshot snapshot) {
        String mutating = state.mutatingId().orElse(null);
        List<AccountAccessRow> rows = new ArrayList<>();
        snapshot.items().forEach(entry ->
                rows.add(new AccountAccessRow(
                        entry,
                        entry.id().equals(mutating))));
        adapter.submitList(List.copyOf(rows));
        boolean empty = snapshot.items().isEmpty();
        binding.accountAccessList.setVisibility(
                empty ? View.GONE : View.VISIBLE);
        binding.accountAccessEmptyGroup.setVisibility(
                empty ? View.VISIBLE : View.GONE);
        if (empty) {
            binding.accountAccessEmptyTitle.setText(
                    snapshot.view() == AccountAccessView.DEVICES
                            ? R.string.account_access_empty_devices_title
                            : R.string.account_access_empty_sessions_title);
            binding.accountAccessEmptyMessage.setText(
                    snapshot.view() == AccountAccessView.DEVICES
                            ? R.string.account_access_empty_devices_message
                            : R.string.account_access_empty_sessions_message);
        }
        binding.accountAccessLoadMore.setVisibility(
                !empty && snapshot.hasMore()
                        ? View.VISIBLE
                        : View.GONE);
        binding.accountAccessLoadMore.setEnabled(
                state.phase()
                        != AccountAccessState.Phase.LOADING_MORE);
        if (state.failure().isPresent()) {
            binding.accountAccessNotice.setText(
                    AccountAccessText.failure(
                            state.failure().orElseThrow()));
            binding.accountAccessNoticeCard.setVisibility(
                    View.VISIBLE);
            support(state);
        }
    }

    private void emptyState() {
        adapter.submitList(List.of());
        binding.accountAccessList.setVisibility(View.GONE);
        binding.accountAccessLoadMore.setVisibility(View.GONE);
        binding.accountAccessEmptyGroup.setVisibility(View.GONE);
    }

    private void select(AccountAccessView view) {
        int expected = view == AccountAccessView.DEVICES
                ? R.id.account_access_devices
                : R.id.account_access_sessions;
        if (binding.accountAccessViews.getCheckedButtonId()
                != expected) {
            binding.accountAccessViews.check(expected);
        }
    }

    private void support(AccountAccessState state) {
        state.requestId().ifPresent(value -> {
            binding.accountAccessNotice.setText(
                    binding.getRoot().getContext().getString(
                            R.string.account_access_request_id,
                            value));
            binding.accountAccessNoticeCard.setVisibility(
                    View.VISIBLE);
        });
    }

    private static boolean busy(AccountAccessState state) {
        return state.phase() == AccountAccessState.Phase.LOADING
                || state.phase()
                        == AccountAccessState.Phase.REFRESHING
                || state.phase()
                        == AccountAccessState.Phase.LOADING_MORE
                || state.phase()
                        == AccountAccessState.Phase.REVOKING;
    }
}
